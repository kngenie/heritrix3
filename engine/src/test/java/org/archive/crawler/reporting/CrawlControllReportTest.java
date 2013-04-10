/**
 *
 */
package org.archive.crawler.reporting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.openmbean.CompositeData;

import org.apache.commons.collections.Closure;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.archive.bdb.BdbModule;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlStatus;
import org.archive.crawler.frontier.DelayedWorkQueue;
import org.archive.crawler.frontier.WorkQueue;
import org.archive.crawler.frontier.WorkQueueFrontier;
import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.crawler.spring.SheetOverlaysManager;
import org.archive.crawler.util.MemUriUniqFilter;
import org.archive.modules.CandidateChain;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.ServerCache;
import org.archive.net.UURIFactory;
import org.archive.spring.ConfigPath;
import org.archive.util.ObjectIdentityCache;
import org.archive.util.Supplier;
import org.archive.util.TmpDirTestCase;

import com.sleepycat.je.DatabaseException;

/**
 *
 * class under test: {@link ToeThreadsReport}, {@link ProcessorsReport}, {@link FrontierSummaryReport},
 * {@link FrontierNonemptyReport}
 *
 * Note this test set currently covers basic template syntax check only.
 * It doesn't even check output from template.
 * More elaborate fixture setup would be necessary for testing WorkQueue information dump etc.
 *
 * @author Kenji Nagahashi
 *
 */
public class CrawlControllReportTest extends TmpDirTestCase {
	@SuppressWarnings("serial")
	public class TestCrawlController extends CrawlController {
		public TestCrawlController() {
		}
		@Override
		public CrawlMetadata getMetadata() {
			CrawlMetadata metadata = new CrawlMetadata();
			metadata.setJobName("test");
			return metadata;
		}
		public void startToeThreads() {
			// start() needs to be run in a thread in AlertThreadGroup, or
			// setupToePool() will fail with NullPointerException.
			setMaxToeThreads(5);
			AlertThreadGroup atg = new AlertThreadGroup("test");
			Thread th = new Thread(atg, "launcher") {
				public void run() { TestCrawlController.this.start(); }
			};
			th.start();
			try {
				th.join();
			} catch (InterruptedException ex) {
				fail("launch thread got interrupted");
			}
			setupToePool();
		}
		@Override
		protected void sendCrawlStateChangeEvent(State newState,
				CrawlStatus status) {
			// noop
		}
	}
	public class TestFrontier extends WorkQueueFrontier /*AbstractFrontier*/ {
		SortedMap<Integer, Queue<String>> inactiveQueuesByPrecedence
			= new TreeMap<Integer, Queue<String>>();
		Queue<String> retiredQueue = new ArrayDeque<String>();
		public TestFrontier() {
			this.serverCache = new ServerCache() {
				@Override
				public Set<String> hostKeys() { return null; }
				@Override
				public CrawlServer getServerFor(String serverKey) { return null; }
				@Override
				public CrawlHost getHostFor(String host) { return null; }
				@Override
				public void forAllHostsDo(Closure action) { }
			};
		}
		@Override
		protected CrawlURI findEligibleURI() {
			// Block ToeThread indefinitely.
			for (;;) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
				}
			}
		}
		@Override
		public CompositeData getURIsList(String marker, int numberOfMatches,
				String regex, boolean verbose) {
			return null;
		}
		@Override
		public long discoveredUriCount() {
			return 0;
		}
		@Override
		public long deepestUri() {
			return 0;
		}
		@Override
		public long averageDepth() {
			return 0;
		}
		@Override
		public float congestionRatio() {
			return 0;
		}
		@Override
		public long deleteURIs(String queueRegex, String match) {
			return 0;
		}
		@Override
		public void deleted(CrawlURI curi) {
		}
		@Override
		public void considerIncluded(CrawlURI curi) {
		}
		@Override
		public FrontierGroup getGroup(CrawlURI curi) {
			// XXX - BdbFrontier has this code. move it to WorkQueueFrontier.
			return getQueueFor(curi.getClassKey());
		}
		@Override
		protected void processFinish(CrawlURI caUri) {
		}
		@Override
		protected int getInProcessCount() {
			return 0;
		}
		@Override
		protected long getMaxInWait() {
			return 0;
		}
		@Override
		protected void initAllQueues() throws DatabaseException {
			allQueues = new ObjectIdentityCache<WorkQueue>() {
				Map<String, WorkQueue> queues = new HashMap<String, WorkQueue>();
				@Override
				public void sync() {
				}
				@Override
				public int size() {
					return queues.size();
				}
				@Override
				public Set<String> keySet() {
					return queues.keySet();
				}
				@Override
				public WorkQueue getOrUse(String key,
						Supplier<WorkQueue> supplierOrNull) {
					if (queues.containsKey(key))
						return queues.get(key);
					else {
						WorkQueue wq = null;
						if (supplierOrNull != null) {
							wq = supplierOrNull.get();
							if (wq != null) {
								queues.put(key, wq);
								wq.setIdentityCache(this);
							}
						}
						return wq;
					}
				}

				@Override
				public WorkQueue get(String key) {
					return queues.get(key);
				}
				@Override
				public void dirtyKey(String key) {
				}
				@Override
				public void close() {
				}
			};
		}
		@Override
		protected SortedMap<Integer, Queue<String>> getInactiveQueuesByPrecedence() {
			return inactiveQueuesByPrecedence;
		}
		@Override
		protected Queue<String> createInactiveQueueForPrecedence(int precedence) {
			return new LinkedList<String>();
		}
		@Override
		protected Queue<String> getRetiredQueues() {
			return retiredQueue;
		}
		@Override
		protected WorkQueue getQueueFor(final String classKey) {
			WorkQueue wq = allQueues.getOrUse(
					classKey,
					new Supplier<WorkQueue>() {
						public WorkQueue get() {
							WorkQueue q = new TestWorkQueue(classKey);
							getQueuePrecedencePolicy().queueCreated(q);
							return q;
						}
					});
			return wq;
		}
		@Override
		protected boolean workQueueDataOnDisk() {
			return false;
		}
		@Override
		protected void initOtherQueues() throws DatabaseException {
			readyClassQueues = new LinkedBlockingQueue<String>();
			snoozedClassQueues = new DelayQueue<DelayedWorkQueue>();
		}
	}
    public class TestBdbModule extends BdbModule {
        public TestBdbModule(File testDir) throws IOException {
            ConfigPath basePath = new ConfigPath("testBase", testDir.getAbsolutePath());
            ConfigPath bdbDir = new ConfigPath("bdb", "bdb");
            bdbDir.setBase(basePath);
            FileUtils.deleteDirectory(bdbDir.getFile());
            setDir(bdbDir);
        }
    }
    @SuppressWarnings("serial")
	public class TestUriUniqFilter extends MemUriUniqFilter {
		public TestUriUniqFilter() {
			// fix MemUriUniqFilter - apparently it wasn't updated
			// when SetBasedUriUniqFilter class was changed.
			createUriSet();
		}
	}
	public class TestWorkQueue extends WorkQueue {
		Queue<CrawlURI> uris = new LinkedList<CrawlURI>();
		public TestWorkQueue(String classKey) {
			super(classKey);
		}
		@Override
		protected void insertItem(WorkQueueFrontier frontier, CrawlURI curi,
				boolean overwriteIfPresent) {
			uris.add(curi);
		}

		@Override
		protected long deleteMatchingFromQueue(WorkQueueFrontier frontier,
				String match) {
			throw new NotImplementedException("deleteMatchingFromQueue");
		}

		@Override
		protected void deleteItem(WorkQueueFrontier frontier, CrawlURI item)
				throws IOException {
			uris.remove(item);
		}

		@Override
		protected CrawlURI peekItem(WorkQueueFrontier frontier)
				throws IOException {
			return uris.peek();
		}
    }

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
	}

	protected String getReportOutput(FreeMarkerReport report, CrawlController controller) {
		StringWriter output = new StringWriter();
		PrintWriter writer = new PrintWriter(output);
		report.write(writer, controller);

		writer.flush();
		return output.toString();
	}
	public void testToeThreadsReport_WithoutToeThreads() {
		CrawlController controller = new CrawlController();
		controller.start();

		FreeMarkerReport report = new ToeThreadsReport();
		String out = getReportOutput(report, controller);
		assertEquals("no ToeThreads\n", out);
	}

	public void testToeThreadReport_WithToeThreads() {
		TestCrawlController controller = new TestCrawlController();
		TestFrontier frontier = new TestFrontier();
		controller.setFrontier(frontier);
		controller.startToeThreads();
		//controller.requestCrawlStart(); // won't work without Spring ApplicationContext.

		FreeMarkerReport report = new ToeThreadsReport();
		String out = getReportOutput(report, controller);
		System.out.println(out);
		assertTrue(out != null && !out.equals(""));
	}

	public void testProcessorReport_NoChains() {
		TestCrawlController controller = new TestCrawlController();

		ProcessorsReport report = new ProcessorsReport();
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	public void testProcessorReport() {
		TestCrawlController controller = new TestCrawlController();
		CandidateChain cc = new CandidateChain();
		// must set, or ProcessorChain.size() will fail with NullPointerException.
		cc.setProcessors(new ArrayList<Processor>());
		controller.setCandidateChain(cc);

		FreeMarkerReport report = new ProcessorsReport();
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	protected TestFrontier setupTestFrontier(CrawlController controller) throws URIException {
		TestFrontier frontier = new TestFrontier();
		frontier.setRecoveryLogEnabled(false);
		frontier.setCrawlController(controller);
		frontier.setUriUniqFilter(new TestUriUniqFilter());
		frontier.setSheetOverlaysManager(new SheetOverlaysManager());
		frontier.setFrontierPreparer(new FrontierPreparer());
		controller.setFrontier(frontier);
		frontier.start();
		assertNotNull(controller.getFrontier());
		assertNotNull(((TestFrontier)controller.getFrontier()).shortReportMap());

		CrawlURI curi = new CrawlURI(UURIFactory.getInstance("http://archive.org/"), "", null, null);
		frontier.schedule(curi);
		return frontier;
	}

	public void testFrontierSummaryReport() throws URIException {
		TestCrawlController controller = new TestCrawlController();
		TestFrontier frontier = setupTestFrontier(controller);

		FreeMarkerReport report = new FrontierSummaryReport();
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	public void testFrontierNonemptyReport() throws URIException {
		TestCrawlController controller = new TestCrawlController();
		TestFrontier frontier = setupTestFrontier(controller);

		FreeMarkerReport report = new FrontierNonemptyReport();
		String out = getReportOutput(report, controller);
		System.out.println(out);

		// some obvious checks
		assertFalse("output should not be 'frontier unstarted'", "frontier unstarted\n".equals(out));
	}
}
