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
package org.archive.modules.hq;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.CrawlUriReceiver;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.spring.KeyedProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;

/**
 * {@link OnlineCrawlMapper} communicates with central <i>crawl headquarter</i>
 * server to pass around URIs among crawl nodes working on the same crawl job.
 * <p>combined with {@link HttpPersistProcessor}, {@link OnlineCrawlMapper} can support de-duplication
 * based on last-modified and content-digest.</p>
 * <p>{@link OnlineCrawlMapper} route all {@link CrawlURI} coming through {@link #add(String, CrawlURI)}
 * to central server. central server performs already-seen check and crawl mapping and dispatches URI
 * to each nodes participating in the crawl job.</p>
 * <p>URI is not dispensed to {@link CrawlUriReceiver} ({@link Frontier}) until explicitly requested with
 * {@link #requestFlush()}.</p>
 * <p>server communication is delegated to {@link HttpHeadquarterAdapter} and it talks to server over HTTP.
 * I should define an interface so that different (more efficient) base protocol may be used and for unit
 * test purposes.</p>
 * @see HttpPersistProcessor
 * @contributor kenji
 */
public class OnlineCrawlMapper implements UriUniqFilter, Lifecycle, CrawlUriReceiver {
    private static final Logger logger = Logger.getLogger(OnlineCrawlMapper.class.getName());
    
    public static final long RETRY_INTERVAL_MS = 30*1000;
    
    private HttpHeadquarterAdapter client;
    
    private int nodeNo = 0;
    
    private int totalNodes = 1;
    
    protected CrawlUriReceiver receiver;
    
    protected FrontierPreparer preparer;
    
    protected CrawlMetadata crawlInfo;
    
    protected final AtomicLong addedCount = new AtomicLong();
    
    protected int discoveredQueueLimit = 2000;
    /**
     * maximum size of discovered URI queue.
     * {@linkplain OnlineCrawlMapper} allows crawl workers to proceed until URI queue
     * reaches this length. When queue fills up, workers will be blocked and thus crawl slows down.
     * higher limit can avoid this situation, but comes with more risk of losing discovered URIs upon
     * crawler crash.
     * note that change after {@link #start()} has no effect.
     * @param discoveredQueueLimit 
     * @see #discoveredBatchSize
     */
    public void setDiscoveredQueueLimit(int discoveredQueueSize) {
        this.discoveredQueueLimit = discoveredQueueSize;
    }
    public int getDiscoveredQueueLimit() {
        return discoveredQueueLimit;
    }
    protected final BlockingQueue<CrawlURI> discoveredQueue;
    /**
     * current size of discovered URI queue.
     * @return int
     */
    public int getDiscoveredQueueSize() {
        return discoveredQueue != null ? discoveredQueue.size() : 0;
    }

    protected final AtomicBoolean discoveredSending = new AtomicBoolean(false);
    
    protected int discoveredBatchSize = 100;
    protected int discoveredBatchSizeMargin = 50;
    
    protected Thread discoveredFlushThread;
    
    protected UriUniqFilter localUniqFilter;
    
    /**
     * maximum number of URIs to request in "feed" request.
     * TODO: need to adjust based on the crawling speed for optimum performance.
     */
    protected int feedBatchSize = 40;
    
    protected volatile boolean running;
    
    private final AtomicLong discoveredSubmitCount = new AtomicLong(0);
    public long getDiscoveredSubmitCount() {
        return discoveredSubmitCount.get();
    }
    private final AtomicLong discoveredSubmitMS = new AtomicLong(0);
    public long getDiscoveredSubmitMS() {
        return discoveredSubmitMS.get();
    }
    
    public class Submitter implements Runnable {
        @Override
        public void run() {
            CrawlURI[] bucket = null;
            while (running || !discoveredQueue.isEmpty()) {
                int bucketSize = discoveredBatchSize + discoveredBatchSizeMargin;
                if (bucket == null || bucket.length != bucketSize)
                    bucket = new CrawlURI[bucketSize];
                Arrays.fill(bucket, null);
                int count = 0;
                for (int i = 0; i < bucket.length; i++) {
                    // wait up to 2 seconds to fill up discoveredBatchSize
                    try {
                        long wait = i < discoveredBatchSize ? 1 : 0;
                        CrawlURI curi = discoveredQueue.poll(wait, TimeUnit.SECONDS);
                        if (curi == null) break;
                        bucket[i] = curi;
                        count++;
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                if (count > 0) {
                    int retries = 0;
                    while (true) {
                        if (logger.isLoggable(Level.FINE)) {
                            if (retries > 0)
                                logger.fine("submiting discovered (retry " + retries + ")");
                            else
                                logger.fine("submitting discovered");
                        }
                        long t0 = System.currentTimeMillis();
                        try {
                            client.mdiscovered(bucket);
                            long ms = System.currentTimeMillis() - t0;
                            discoveredSubmitMS.addAndGet(ms);
                            if (logger.isLoggable(Level.FINE))
                                logger.fine("submission done in " + ms
                                        + "ms, discoveredQueue.size="
                                        + discoveredQueue.size());
                            discoveredSubmitCount.addAndGet(count);
                            break;
                        } catch (IOException ex) {
                            logger.warning("submitting discovered failed: " + ex);
                        }
                        if (!running) {
                            logger.severe("discovered did not complete, lost URLs:");
                            int i = 0;
                            for (CrawlURI curi : bucket) {
                                if (curi == null) continue;
                                i++;
                                logger.severe("  " + i + ":" + curi.getURI());
                            }
                            break;
                        }
                        retries++;
                        try {
                            Thread.sleep(RETRY_INTERVAL_MS);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }
    
    public OnlineCrawlMapper() {
        //this.discoveredQueue = new ConcurrentLinkedQueue<CrawlURI>();
        this.discoveredQueue = new LinkedBlockingQueue<CrawlURI>(500);
    }
    
    @Autowired(required=true)
    public void setClient(HttpHeadquarterAdapter client) {
        this.client = client;
    }
    public void setDestination(CrawlUriReceiver receiver) {
        this.receiver = receiver;
    }
    @Autowired(required=true)
    public void setPreparer(FrontierPreparer preparer) {
        this.preparer = preparer;
    }
    @Autowired(required=true)
    public void setCrawlMetadata(CrawlMetadata crawlInfo) {
        this.crawlInfo = crawlInfo;
    }
    /**
     * a {@link UriUniqFilter} for local <i>seen</i>-check to reduce
     * <i>discovered</i> traffic to the Headquarters (optional).
     * @param localUniqFilter UriUniqFilter
     */
    public void setLocalUniqFilter(UriUniqFilter localUniqFilter) {
        if (this.localUniqFilter != null)
            this.localUniqFilter.setDestination(null);
        this.localUniqFilter = localUniqFilter;
        if (this.localUniqFilter != null) {
            this.localUniqFilter.setDestination(this);
        }
    }
    
    public int getNodeNo() {
        return nodeNo;
    }
    /**
     * number that uniquely identifies this node within crawl job cluster.
     * @param nodeNo non-negative integer.
     */
    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public int getTotalNodes() {
        return totalNodes;
    }
    /**
     * total number of crawl job cluster nodes.
     * @param totalNodes positive integer.
     * @deprecated this parameter is not used by recent HQ server.
     */
    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }
    /**
     * baseline batch size of discovered URI submission. {@linkplain OnlineCrawlMapper} waits for
     * incoming discovered URI with timeout of 1 second (non-configurable at this moment) until submission
     * batch reaches this size. after reaching this size, {@linkplain OnlineCrawlMapper} submits the
     * batch as soon as incoming queue becomes empty. For best performance, discoveredQueueLimit should
     * be larger than discoveredBatchSize.
     * can be changed while job is running. it will be reflected to the next submission cycle.
     * @param discoveredBatchSize
     * @see #setDiscoveredQueueLimit(int)
     * @see #setDiscoveredBatchSizeMargin(int)
     */
    public void setDiscoveredBatchSize(int discoveredBatchSize) {
        this.discoveredBatchSize = discoveredBatchSize;
    }
    public int getDiscoveredBatchSize() {
        return discoveredBatchSize;
    }
    /**
     * maximum number of discovered URIs put into submission batch <i>beyond</i> discoveredBatchSize.
     * when batch size reaches discoveredBatchSize + discoveredBatchSizeMargin, submission is forced
     * even when more discovered URIs are waiting in the queue.
     * @param discoveredBatchSizeMargin
     * @see #setDiscoveredBatchSize(int)
     */
    public void setDiscoveredBatchSizeMargin(int discoveredBatchSizeMargin) {
        this.discoveredBatchSizeMargin = discoveredBatchSizeMargin;
    }
    public int getDiscoveredBatchSizeMargin() {
        return discoveredBatchSizeMargin;
    }
    public void setFeedBatchSize(int feedBatchSize) {
        this.feedBatchSize = feedBatchSize;
    }
    public int getFeedBatchSize() {
        return feedBatchSize;
    }
    
    protected void schedule(CrawlURI curi) {
        // WorkQueueFrontier.processScheduleAlways() (called from receive())
        // assumes CrawlURI.classKey is non-null. schedule() calls prepare()
        // if CrawlURI.classKey is null, but processScheduleAlways() has no such
        // rescue. That rescue code should have been in processScheduleAlways()
        // rather than in schedule().
        if (curi.getClassKey() == null) {
            KeyedProperties.loadOverridesFrom(curi);
            try {
                preparer.prepare(curi);
            } finally {
                KeyedProperties.clearOverridesFrom(curi);
            }
        }
        if (logger.isLoggable(Level.FINE))
            logger.fine("scheduling " + curi + " to receive()");
        receiver.receive(curi);
    }
    
    /**
     * for receiving CrawlURI from {@link #localUniqFilter}.
     * @param item CrawlURI previously unseen.
     */
    @Override
    public void receive(CrawlURI item) {
        queueDiscovered(item);
    }
    
    /**
     * {@inheritDoc}
     * this implementation makes no use of <code>key</code>.
     * this method sends all CrawlURI to headquarters except prerequisites.
     */
    @Override
    public void add(String key, CrawlURI curi) {
        addedCount.incrementAndGet();
        // send all prerequisites (such as DNS lookup) to Frontier.
        // CrawlUri.prerequisite flag is set to true by PreconditionEnforcer
        // when it received dns: CrawlURI, not before dns: CrawlURI is scheduled.
        // we cannot use it for distinguishing prerequisite. Less specific,
        // but forceFetch (forceRevisit) is set for both dns: and robots.txt
        // prerequisite CrawlURI. Possible danger is there are a few other places
        // forceFetch is set to true.
//        if (curi.isPrerequisite()) {
        if (curi.forceFetch()) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("sending " + curi + " to receive()");
            //schedule(curi);
            // at this point, CrawlURI has been initialized by
            // FrontierPreparer. we don't need to do it again.
            receiver.receive(curi);
            return;
        }
        // discard non-fetchable URIs
        String scheme = curi.getUURI().getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme) && !"ftp".equals(scheme)) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("discarding non-fetchable URI:" + curi);
            return;
        }
        
        if (localUniqFilter != null) {
            localUniqFilter.add(key, curi);
        } else {
            queueDiscovered(curi);
        }
    }
    
    private final AtomicLong discoveredQueuedCount = new AtomicLong(0);
    public long getDiscoveredQueuedCount() {
        return discoveredQueuedCount.get();
    }
    
    protected void queueDiscovered(CrawlURI curi) {
        // send all CrawlURI to the central server
        discoveredQueuedCount.incrementAndGet();
        if (running) {
            // flush thread is active. queue and leave.
            if (logger.isLoggable(Level.FINE))
                logger.fine("queueing for submission: " + curi.getURI());
            try {
                discoveredQueue.put(curi);
            } catch (InterruptedException ex) {
                // TODO: do not lose curi!
            }
        } else {
            // paranoia? even after stopping we may receive discovered URIs due
            // to the activity of other components being stopped. forward it to
            // Headquarter one by one so that we don't lose it.
            client.discovered(curi);
        }
    }
    
    private final Lock feedLock = new ReentrantLock();
    
    private final AtomicLong feedCount = new AtomicLong(0);
    public long getFeedCount() {
        return feedCount.get();
    }
    
    @Override
    public long requestFlush() {
        if (feedLock.tryLock()) {
            try {
                int safeTotalNodes = totalNodes > 0 ? totalNodes : 1;
                if (logger.isLoggable(Level.FINE))
                    logger.fine("running getCrawlURIs()");
                long t0 = System.currentTimeMillis();
                // TODO: make factor "10" configurable 
                CrawlURI[] uris = client.getCrawlURIs(nodeNo, safeTotalNodes, feedBatchSize);
                if (logger.isLoggable(Level.FINE))
                    logger.fine("getCrawlURIs() done in " + (System.currentTimeMillis() - t0) +
                            "ms, " + uris.length + " URIs");
                long count = 0;
                for (CrawlURI uri : uris) {
                    if (uri != null) {
                        schedule(uri);
                        count++;
                    }
                }
                feedCount.addAndGet(count);
                return count;
            } finally {
                feedLock.unlock();
            }
        } else {
            return 0;
        }
    }

    @Override
    public void addForce(String key, CrawlURI value) {
        add(key, value);
    }

    @Override
    public void addNow(String key, CrawlURI value) {
        add(key, value);
    }

    @Override
    public void close() {
        // nothing's necessary.
    }

    /**
     * not really implemented. always returns 0.
     */
    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long addedCount() {
        return addedCount.get();
    }
    
    /**
     * count of {@link CrawlURI} added.
     * for JavaBean access (specifically JMX).
     * @return count
     * @see #add(String, CrawlURI)
     */
    public long getAddedCount() {
        return addedCount.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void forget(String key, CrawlURI value) {
        // TODO Auto-generated method stub
        // this method is called by WorkQueueFrontier#forget(CrawlURI), but that method
        // is not used anywhere in java code. probably for use via bean shell?
        // anyway no implementation since headquarter is supposed to provide this functionality.
    }

    @Override
    public void note(String key) {
        // TODO Auto-generated method stub
        // used in WorkQueueFrontier#considerIncluded(CrawlURI), which is used in
        // AbstractFrontier#importRecoverFormat(File, boolean, boolean, String).
        // considerIncluded() passes CrawlURI.getCanonicalString() as key.
        // For it's supposed we no longer need local recovery file in cluster configuration with
        // headquarter, this method is unimplemented.
    }

    /**
     * {@inheritDoc}
     * always returns 1.
     * as this method is used by {@link org.archive.crawler.frontier.WorkQueueFrontier} to
     * decide if crawl job reached its end, it means HQ-based crawl never ends.
     * <p>In the future, HQ may send a message (in response to feed request) to signify job end.</p>
     */
    @Override
    public long pending() {
        return 1;
    }

    @Override
    public void setProfileLog(File logfile) {
        // TODO Auto-generated method stub
        // currently unimplemented.
    }
    /**
     * start discovered URI submission thread.
     * this method is public for emergency use (currently submission thread exits upon
     * serious error - maybe it shouldn't).
     * request is ignored if submission threads is running.
     */
    public void startDiscoveredSubmitter() {
        if (discoveredFlushThread != null && discoveredFlushThread.isAlive()) return;
        discoveredFlushThread = new Thread(new Submitter());
        discoveredFlushThread.setName("DiscoveredSubmitter");
        discoveredFlushThread.start();        
    }

    // Lifecycle
    
    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        if (running) return;
        if (localUniqFilter instanceof Lifecycle) {
            ((Lifecycle)localUniqFilter).start();
        }
        running = true;
        startDiscoveredSubmitter();
    }

    @Override
    public void stop() {
        running = false;
        client.reset(nodeNo, totalNodes);
        discoveredFlushThread = null;
        if (localUniqFilter instanceof Lifecycle) {
            ((Lifecycle)localUniqFilter).stop();
        }
    }
    
}
