/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.crawler.frontier;

import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.ToeThread;
import org.archive.modules.CrawlURI;
import org.archive.spring.KeyedProperties;

/**
 * Frontier with following modifications packaged as sub class of BdbFrontier:
 * <ol>
 * <li>'Pulls' more URIs when readyQueue becomes low by calling
 * {@link UriUniqFilter#requestFlush()}.</li>
 * <li>Uses simpler mechanism for RUN/PAUSE control.</li>
 * <li>wakes up snoozed queue in managementTasks() instead of findEligibleURI.</li>
 * </ol>
 * Pulling is for implementing crawler cluster receiving
 * CrawlURIs from a remote queue management server.
 * Other modifications are for better performance and robustness.
 * This class is still experimental. 
 * @contributor kenji
 */
public class PullingBdbFrontier extends BdbFrontier {
    private static Logger logger = Logger.getLogger(PullingBdbFrontier.class.getName());
    
    protected Lock readyLock;
    protected Condition queueReady;
    
    protected Lock snoozeLock;
    protected Condition snoozeUpdated;
    private long nextWakeTime = Long.MAX_VALUE;
    
    public PullingBdbFrontier() {
        this.readyLock = new ReentrantLock();
        this.queueReady = this.readyLock.newCondition();
        this.snoozeLock = new ReentrantLock();
        this.snoozeUpdated = this.snoozeLock.newCondition();
    }
    
    /**
     * exposes {@link UriUniqFilter#addedCount()} for monitoring framework.
     * @return
     */
    public long candidateUriCount() {
        return (this.uriUniqFilter != null) ? this.uriUniqFilter.addedCount() : 0;
    }
    
    /**
     * call this method to notify management thread of a queue being
     * snoozed. management thread will wake up and recalculate next wake up
     * time if necessary.
     * @param wakeTime
     */
    protected void updateSnoozeTime(long wakeTime) {
        if (wakeTime < nextWakeTime) {
            snoozeLock.lock();
            try {
                snoozeUpdated.signal();
            } finally {
                snoozeLock.unlock();
            }
        }
    }
    @Override
    public void requestState(State target) {
        super.requestState(target);
        // notify state change
        if (target != lastReachedState) {
            if (targetState == State.RUN) {
                readyLock.lock();
                // resume just one thread and it will resume other threads
                // if more CrawlURIs are available.
                queueReady.signal();
                readyLock.unlock();
            }
            // notify management thread of targetState change.
            snoozeLock.lock();
            snoozeUpdated.signal();
            snoozeLock.unlock();
        }
    }
    
    @Override
    protected void managementTasks() {
        // note: don't use requestState() for updating targetState. it has code for signaling management thread,
        // which is safe but just a waste of CPU cycles if management thread itself is updating targetState.
        while (lastReachedState != State.FINISH) {
            try {
                // perform operations for current target state. note operations is chosen
                // by targetState, not lastReachedState, because operation includes what's
                // needed to reach the targetState.
                long sleepms = 1000L;
                switch (targetState) {
                case EMPTY:
                    if (!isEmpty()) {
                        targetState = State.RUN;
                        sleepms = 0;
                        break;
                    }
                    reachedState(State.EMPTY);
                    // wake up one ToeThread to pull CURLs
                    readyLock.lock();
                    try {
                        queueReady.signal();
                    } finally {
                        readyLock.unlock();
                    }
                    break;
                case RUN:
                    if (isEmpty()) {
                        targetState = State.EMPTY;
                        sleepms = 0;
                        break;
                    }
                    reachedState(State.RUN);
                    // move queues who finished snoozing to ready queue then sleep until
                    // the time for the next wake up call.
                    wakeQueues();
                    // TODO: we need to take queues in snoozedOverflow into account for completeness
                    DelayedWorkQueue head = snoozedClassQueues.peek();
                    long nextWake = head != null ? head.getWakeTime() : Long.MAX_VALUE;
                    sleepms = nextWake - System.currentTimeMillis();
                    if (sleepms > 1000L) sleepms = 1000L;
                    // TODO: risk of other threads' reading partially updated value?
                    // false alarm from updateSnoozeTime doesn't hurt.
                    nextWakeTime = nextWake;
                    break;
                case HOLD:
                case PAUSE:
                    if (targetState != lastReachedState) {
                        if (getInProcessCount() == 0)
                            reachedState(State.PAUSE);
                    }
                    break;
                case FINISH:
                    if (targetState != lastReachedState) {
                        if (getInProcessCount() == 0) {
                            finalTasks();
                            reachedState(State.FINISH);
                            sleepms = 0;
                        }
                    }
                    break;
                }
                if (sleepms > 0) {
                    snoozeLock.lock();
                    try {
                        snoozeUpdated.await(sleepms, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        //
                    } finally {
                        snoozeLock.unlock();
                    }
                }
            } catch (RuntimeException ex) {
                // log, try to pause, continue
                logger.log(Level.SEVERE, "", ex);
                if (targetState!=State.PAUSE && targetState!=State.FINISH) {
                    targetState = State.PAUSE;
                }
            }
        }
        logger.log(Level.FINE, "ending frontier mgr thread");
    }
   
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#next()
     */
    public CrawlURI next() throws InterruptedException {
        long t0 = System.currentTimeMillis();
        
        CrawlURI crawlable = null;
        do {
            if (targetState == State.RUN) {
                long t1 = System.currentTimeMillis();
                if (logger.isLoggable(Level.FINE))
                    logger.fine("calling findEligibleURI()");
                crawlable = findEligibleURI();
                if (logger.isLoggable(Level.FINE))
                    logger.fine(String.format("findEligibleURI() done in %dms",
                            System.currentTimeMillis() - t1));
            }
            // if CrawlURI is unavailable due to readyQueue exhaustion or PAUSED state
            // sleep until notified of situation change.
            if (crawlable == null) {
                readyLock.lock();
                try {
                    queueReady.await();
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("resumed");
                } finally {
                    readyLock.unlock();
                }
            }
        } while (crawlable == null);
        
        if (logger.isLoggable(Level.FINE))
            logger.fine(String.format("got URI in %dms",
                    System.currentTimeMillis() - t0));
        return crawlable;
    }
    
    /**
     * Put the given queue on the readyClassQueues queue.
     * <p>it will release {@link ToeThread}s waiting on readyClassQueues.
     * see {@link #findEligibleURI()}. It is no longer necessary to
     * call {@link #findEligibleURI()} to fill outbound queue. Waking up
     * {@link ToeThread} handles it.</p>
     * @param wq {@link WorkQueue} to become ready.
     */
    @Override
    protected void readyQueue(WorkQueue wq) {
        try {
            readyClassQueues.put(wq.getClassKey());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "queue readied: " + wq.getClassKey());
            }
            queueReadiedCount.incrementAndGet();
            readyLock.lock();
            try {
                queueReady.signal();
            } finally {
                readyLock.unlock();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "unable to add " + wq.getClassKey() + " to readyClassQueue", e);
            // propagate interrupt up 
            throw new RuntimeException(e);
        }
    }

    private int pullTriggerLevel = 100;
    public void setPullTriggerLevel(int pullTriggerLevel) {
        this.pullTriggerLevel = pullTriggerLevel;
    }
    public int getPullTriggerLevel() {
        return pullTriggerLevel;
    }
    private final AtomicBoolean pulling = new AtomicBoolean();

    /**
     * called when readyClassQueue becomes low, pulls more URIs into crawler.
     * @return true if this thread should check the level again.
     */
    protected boolean pullURIs() {
        // we want only one thread work on this task. other thread should go ahead and
        // wait on readyClassQueue.
        if (!pulling.compareAndSet(false, true)) return false;

        try {
            // first try pulling from local InactiveQueue
            if (!getInactiveQueuesByPrecedence().isEmpty() && highestPrecedenceWaiting < getPrecedenceFloor()) {
                if (activateInactiveQueue()) {
                    return true;
                }
            }
            // then trigger pull from external source.
            long t2 = System.currentTimeMillis();
            long nflushed = uriUniqFilter.requestFlush();
            if (logger.isLoggable(Level.FINE))
                logger.fine("UriUniqFilter flushed: "
                        + nflushed + " in "
                        + (System.currentTimeMillis() - t2)
                        + "ms");
            if (nflushed == 0) return false;
            //           if (nflushed > 0) {
            //               // if URLs got flushed, check readyClassQueue again.
            //               // if no queues are ready, probably all URLs have gone
            //               // into existing snoozed queues. retry flushing again,
            //               // but allow operator to pause the job by continuing outer
            //               // loop rather than looping here.
            //               readyLock.lock();
            //               try { queueReady.signalAll(); }
            //               finally { readyLock.unlock(); }
            //           }
        } finally {
            pulling.set(false);
        }
        return true;
    }

    /**
     * Return the next CrawlURI eligible to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * Relies on the readyClassQueues having been loaded with
     * any work queues that are eligible to provide a URI. 
     *
     * @return next CrawlURI eligible to be processed, or null if none available
     *
     * @see org.archive.crawler.framework.Frontier#next()
     */
    protected CrawlURI findEligibleURI() {
        // wake any snoozed queues - this is now done by managementTasks()
        //wakeQueues();
        // consider rescheduled URIS
        checkFutures();

        CrawlURI curi = null;
        // TODO: refactor to untangle these loops, early-exits, etc!
        findauri: while (true) {
            // find a non-empty ready queue, if any
            WorkQueue readyQ = null;
            do {
                String key = null;
                do {
                    if (targetState != State.RUN && targetState != State.EMPTY) return null;
                    // if size of readyClassQueue dropped below configured
                    // level, trigger "pulling" so that more queues will
                    // become ready before readyClassQueue gets exhausted.
                    int readyLevel = readyClassQueues.size();
                    if (readyLevel < pullTriggerLevel || (key = readyClassQueues.poll()) == null) {
                        if (pullURIs()) continue;
                        return null;
                    }
                } while (key == null);
                if (logger.isLoggable(Level.FINE))
                    logger.fine("found ready queue: " + key);
                readyQ = getQueueFor(key);
                if (readyQ == null) {
                    // readyQ key wasn't in all queues: unexpected, but manageable.
                    logger.warning("Key " + key + " in readyClassQueue, but not in allQueues");
                    readyQ = null;
                    continue;
                }
                if (readyQ.getCount() == 0) {
                    // readyQ is empty and ready: it's exhausted
                    if (logger.isLoggable(Level.FINE))
                        logger.fine(readyQ.getClassKey()
                                + " is exhausted, trying another");
                    readyQ.noteExhausted();
                    readyQ.makeDirty();
                    readyQ = null;
                    continue;
                }
                if (!inProcessQueues.add(readyQ)) {
                    // double activation; discard this and move on
                    // (this guard allows other enqueuings to ready or
                    // the various inactive-by-precedence queues to
                    // sometimes redundantly enqueue a queue key)
                    if (logger.isLoggable(Level.FINE))
                        logger.fine(readyQ.getClassKey() + " is in-process");
                    readyQ = null;
                    continue;
                }
                // queue has gone 'in process'
                readyQ.considerActive();
                readyQ.setWakeTime(0); // clear obsolete wake time, if any

                readyQ.setSessionBudget(getBalanceReplenishAmount());
                readyQ.setTotalBudget(getQueueTotalBudget());
                if (readyQ.isOverSessionBudget()) {
                    deactivateQueue(readyQ);
                    readyQ.makeDirty();
                    readyQ = null;
                    continue;
                }
                if (readyQ.isOverTotalBudget()) {
                    retireQueue(readyQ);
                    readyQ.makeDirty();
                    readyQ = null;
                    continue;
                }
            } while (readyQ == null);

            if (readyQ == null) {
                // no queues left in ready or readiable
                break findauri;
            }

            returnauri: while (true) { // loop left by explicit return or break
                // on empty
                curi = readyQ.peek(this);
                if (curi == null) {
                    // should not reach
                    logger.severe("No CrawlURI from ready non-empty queue "
                            + readyQ.getClassKey() + "\n"
                            + readyQ.shortReportLegend() + "\n"
                            + readyQ.shortReportLine() + "\n");
                    break returnauri;
                }

                // from queues, override names persist but not map source
                curi.setOverlayMapsSource(sheetOverlaysManager);
                // TODO: consider optimizations avoiding this recalc of
                // overrides when not necessary
                sheetOverlaysManager.applyOverlaysTo(curi);
                // check if curi belongs in different queue
                String currentQueueKey;
                try {
                    KeyedProperties.loadOverridesFrom(curi);
                    currentQueueKey = getClassKey(curi);
                } finally {
                    KeyedProperties.clearOverridesFrom(curi);
                }
                if (currentQueueKey.equals(curi.getClassKey())) {
                    // curi was in right queue, emit
                    noteAboutToEmit(curi, readyQ);
                    // really unnecessary?
                    // inProcessQueues.add(readyQ);
                    // return curi;
                    break findauri;
                }
                if (logger.isLoggable(Level.FINE))
                    logger.fine(readyQ.getClassKey() + " classKey changed "
                            + curi.getClassKey() + "->" + currentQueueKey);

                // URI's assigned queue has changed since it
                // was queued (eg because its IP has become
                // known). Requeue to new queue.
                // TODO: consider synchronization on readyQ
                readyQ.dequeue(this, curi);
                doJournalRelocated(curi);
                curi.setClassKey(currentQueueKey);
                decrementQueuedCount(1);
                curi.setHolderKey(null);
                sendToQueue(curi);
                if (readyQ.getCount() == 0) {
                    // readyQ is empty and ready: it's exhausted
                    // release held status, allowing any subsequent
                    // enqueues to again put queue in ready
                    // FIXME: tiny window here where queue could
                    // receive new URI, be readied, fail not-in-process?
                    inProcessQueues.remove(readyQ);
                    readyQ.noteExhausted();
                    readyQ.makeDirty();
                    readyQ = null;
                    continue findauri;
                }
            }
        }
        return curi;
    }

    @Override
    protected boolean activateInactiveQueue() {
        for (Entry<Integer, Queue<String>> entry: getInactiveQueuesByPrecedence().entrySet()) {
            int expectedPrecedence = entry.getKey();
            Queue<String> queueOfWorkQueueKeys = entry.getValue();

            while (true) {
                synchronized (getInactiveQueuesByPrecedence()) {
                    String workQueueKey = queueOfWorkQueueKeys.poll();
                    if (workQueueKey == null) {
                        break;
                    }

                    WorkQueue candidateQ = (WorkQueue) this.allQueues.get(workQueueKey);
                    if (candidateQ.getPrecedence() > expectedPrecedence) {
                        // queue demoted since placed; re-deactivate
                        deactivateQueue(candidateQ);
                        candidateQ.makeDirty();
                        continue; 
                    }

                    updateHighestWaiting(expectedPrecedence);
                    try {
                        readyClassQueues.put(workQueueKey);
                        // begin diff
                        readyLock.lock();
                        try {
                            queueReady.signal();
                        } finally {
                            readyLock.unlock();
                        }
                        // end diff
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e); 
                    } 

                    return true; 
                }
            }
        }

        return false;
    }

    @Override
    protected void updateHighestWaiting(int startFrom) {
        readyLock.lock();
        try {
            super.updateHighestWaiting(startFrom);
        } finally {
            readyLock.unlock();
        }
    }

    @Override
    protected void snoozeQueue(WorkQueue wq, long now, long delayMs) {
        if (logger.isLoggable(Level.FINE))
            logger.fine("snoozing " + wq.getClassKey() + " for " + delayMs + "ms");
        super.snoozeQueue(wq, now, delayMs);
        // let management thread (possibly waiting on Condition) know that
        // snoozed queues have been changed and perhaps it needs to recalculate
        // sleep time.
        updateSnoozeTime(now + delayMs);
    }

}
