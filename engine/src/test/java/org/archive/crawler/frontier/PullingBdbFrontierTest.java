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

import java.io.File;
import java.io.IOException;
import java.lang.Thread.State;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.archive.bdb.BdbModule;
import org.archive.checkpointing.Checkpoint;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlStatus;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.modules.CrawlURI;
import org.archive.spring.ConfigPath;

/**
 * @contributor kenji
 */
public class PullingBdbFrontierTest extends TestCase {

    PullingBdbFrontier frontier;
    public class MockFrontierPreparer extends FrontierPreparer {
        
    }
    public class MockUriUniqFilter implements UriUniqFilter {
        CrawlUriReceiver receiver;
        long addedCount = 0;
        long pulledCount = 0;
        public MockUriUniqFilter() {
        }
        @Override
        public void add(String key, CrawlURI value) {
            receiver.receive(value);
        }

        @Override
        public long addedCount() {
            return addedCount;
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
        public void close() {}

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void forget(String key, CrawlURI value) {
        }

        @Override
        public void note(String key) {
        }

        @Override
        public long pending() {
            return 0;
        }

        @Override
        public long requestFlush() {
            pulledCount++;
            return 0;
        }

        @Override
        public void setDestination(CrawlUriReceiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public void setProfileLog(File logfile) {}
    }
    MockUriUniqFilter uriUniqFilter;
    
    public class MockCrawlController extends CrawlController {
        Frontier.State frontierState;
        public MockCrawlController(Frontier frontier) {
            setFrontier(frontier);
        }
        @Override
        protected void sendCrawlStateChangeEvent(
                org.archive.crawler.framework.CrawlController.State newState,
                CrawlStatus status) {
            // overridden so that we don't need to setup ApplicationContext
        }
        @Override
        protected void completeStop() {
            // overridden so that we don't need to setup ApplicationContext
        }
        @Override
        public void noteFrontierState(
                org.archive.crawler.framework.Frontier.State reachedState) {
            this.frontierState = reachedState;
        }
    }
    MockCrawlController controller;
    
    public class MockBdbModule extends BdbModule {
        public MockBdbModule() throws IOException {
            FileUtils.deleteDirectory(getDir().getFile());
        }
        public void cleanup() throws IOException {
            FileUtils.deleteDirectory(getDir().getFile());
        }
        /*
        @Override
        public Database openDatabase(String name, BdbConfig config,
                boolean usePriorData) throws DatabaseException {
            return null;
        }
        @Override
        public StoredClassCatalog getClassCatalog() {
            return null;
        }
        @Override
        public <V extends IdentityCacheable> ObjectIdentityCache<V> getObjectCache(
                String dbName, boolean recycle, Class<V> declaredClass,
                Class<? extends V> valueClass) throws DatabaseException {
            return null;
        }
        @Override
        public <K extends Serializable> StoredQueue<K> getStoredQueue(
                String dbname, Class<K> clazz, boolean usePriorData) {
            return null;
        }
        @Override
        public <K, V> DisposableStoredSortedMap<K, V> getStoredMap(
                String dbName, Class<K> keyClass, Class<V> valueClass,
                boolean allowDuplicates, boolean usePriorData) {
            return null;
        }
        */
    }
    MockBdbModule bdbModule;
    /**
     * @param name
     */
    public PullingBdbFrontierTest(String name) {
        super(name);
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        frontier = new PullingBdbFrontier();
        frontier.setFrontierPreparer(new MockFrontierPreparer());
        frontier.setUriUniqFilter(uriUniqFilter = new MockUriUniqFilter());
        frontier.setCrawlController(controller = new MockCrawlController(frontier));
        frontier.setBdbModule(bdbModule = new MockBdbModule());
        /*
        Checkpoint checkpoint = new Checkpoint();
        ConfigPath recoverPath = new ConfigPath("recover", "cp-1");
        checkpoint.setCheckpointDir(recoverPath);
        checkpoint.afterPropertiesSet();
        bdbModule.setRecoveryCheckpoint(checkpoint);
        */
        bdbModule.start();
        
        // non essential -  just for saving unecessary I/O
        frontier.setRecoveryLogEnabled(false);
        
        frontier.start();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        frontier.close();
        frontier.stop();
        bdbModule.stop();
        bdbModule.destroy();
        bdbModule.cleanup();
        super.tearDown();
    }

    public void testInitiallyPaused() throws InterruptedException {
        Thread th = new Thread() {
            public void run() {
                try {
                    frontier.next();
                } catch (InterruptedException ex) {
                }
            };
        };
        th.setDaemon(true);
        th.start();
        Thread.sleep(1000);
        assertEquals("frontier should be in PAUSE state",
                Frontier.State.PAUSE, controller.frontierState);
        State state = th.getState();
        assertEquals("thread should be in WAITING state", State.WAITING, state);
        frontier.requestState(Frontier.State.FINISH);
    }
    
    public void testPull() throws Exception {
        Thread th = new Thread() {
            public void run() {
                try {
                    frontier.next();
                } catch (InterruptedException ex) {
                }
            }
        };
        th.setDaemon(true);
        th.start();
        while (th.getState() != State.WAITING) {
            Thread.sleep(100);
        }
        frontier.requestState(Frontier.State.RUN);
        Thread.sleep(1000);
        // Frontier should pull just once. readyQueue is empty, and
        // MockUriUniqFilter.requestFlush() returns 0.
        assertEquals(1, uriUniqFilter.pulledCount);
        frontier.requestState(Frontier.State.FINISH);
    }
}
