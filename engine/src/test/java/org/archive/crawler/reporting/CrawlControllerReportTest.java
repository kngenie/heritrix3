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
import java.util.Collections;
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

import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.archive.bdb.BdbModule;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.frontier.DelayedWorkQueue;
import org.archive.crawler.frontier.WorkQueue;
import org.archive.crawler.frontier.WorkQueueFrontier;
import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.crawler.spring.SheetOverlaysManager;
import org.archive.crawler.util.MemUriUniqFilter;
import org.archive.modules.CandidateChain;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.DispositionChain;
import org.archive.modules.FetchChain;
import org.archive.modules.Processor;
import org.archive.modules.deciderules.DecideRule;
import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.modules.net.ServerCache;
import org.archive.modules.seeds.SeedModule;
import org.archive.net.UURIFactory;
import org.archive.spring.ConfigPath;
import org.archive.util.ObjectIdentityCache;
import org.archive.util.Supplier;
import org.archive.util.TmpDirTestCase;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sleepycat.je.DatabaseException;

/**
 * TestCase covering multiple reporters implemented with FreeMarker templates.
 * Report templates covered:
 * <ul>
 * <li>{@code ToeThreads.ftl}</li>
 * <li>{@code Processors.ftl}</li>
 * <li>{@code FrontierSummary.ftl}</li>
 * <li>{@code FrontierNonempty.ftl}</li>
 * <li>{@code CrawlSummary.ftl}</li>
 * <li>{@link Mimetypes.ftl}</li>
 * </ul>
 * <p>
 * Note this test set currently covers basic template syntax check only.
 * It doesn't even check output from template.
 * More elaborate fixture setup would be necessary for testing WorkQueue information dump etc.
 * </p>
 * @author Kenji Nagahashi
 *
 */
public class CrawlControllerReportTest extends TmpDirTestCase {
	@Configuration
	public static class TestApp {
		@Bean
		public CrawlController controller() throws Exception {
			TestCrawlController controller = new TestCrawlController();
			// XXX - no effect
			controller.setMaxToeThreads(5);
			return controller;

		}
		@Bean
		public StatisticsTracker stats() {
			StatisticsTracker stats = new StatisticsTracker();
			// disable automatic dumping on stop()
			stats.setReports(Collections.<Report>emptyList());
			return stats;
		}
		@Bean
		public CrawlMetadata metadata() {
			CrawlMetadata metadata = new CrawlMetadata();
			metadata.setJobName("test");
			return metadata;
		}
		@Bean
		public ServerCache serverCache() {
			return new DefaultServerCache();
		}
		@Bean
		public Frontier frontier() throws Exception {
			TestFrontier frontier = new TestFrontier();
			frontier.setRecoveryLogEnabled(false);
			frontier.setSheetOverlaysManager(new SheetOverlaysManager());
			frontier.setFrontierPreparer(new FrontierPreparer());
			return frontier;
		}
		@SuppressWarnings("serial")
		@Bean
		public UriUniqFilter uriUniqFilter() {
			return new MemUriUniqFilter() {
				{
					// TODO: fix MemUriUniqFilter - apparently it wasn't
					// updated when SetBasedUriUniqFilter was changed.
					createUriSet();
				}
			};
		}
		@Bean
		public CandidateChain candidateChain() {
			CandidateChain cc = new CandidateChain();
			// must set, or ProcessorChain.size() will fail with
			// NullPointerException.
			cc.setProcessors(new ArrayList<Processor>());
			return cc;
		}
		@Bean
		public BdbModule bdb() throws Exception {
			BdbModule bdb = new BdbModule();
			ConfigPath basePath = new ConfigPath("testBase", tmpDir()
					.getAbsolutePath());
            ConfigPath bdbDir = new ConfigPath("bdb", "bdb");
            bdbDir.setBase(basePath);
            FileUtils.deleteDirectory(bdbDir.getFile());
            bdb.setDir(bdbDir);
			return bdb;
		}

		@Bean
		public FrontierPreparer frontierPreparer() {
			return new FrontierPreparer();
		}
		@Bean
		public SeedModule seedModule() {
			// cannot be null
			return new SeedModule() {
				@Override
				public void announceSeeds() {
				}
				@Override
				public void actOn(File f) {
				}
				@Override
				public void addSeed(CrawlURI curi) {
				}
			};
		}

		// TODO: It is rather painful to define beans unnecessary for this test,
		// because of @Autowired annotation. Is there a way to get around this?
		@Bean
		public CrawlerLoggerModule loggerModule() {
			return null;
		}
		@Bean
		public DecideRule decideRule() {
			return null;
		}
		@Bean
		public FetchChain fetchChain() {
			return null;
		}
		@Bean
		public DispositionChain dispositionChain() {
			return null;
		}
		@Bean
		public SheetOverlaysManager overlaysManager() {
			return new SheetOverlaysManager();
		}
	}

	@SuppressWarnings("serial")
	public static class TestCrawlController extends CrawlController {
		public TestCrawlController() {
		}

		@Override
		public void logProgressStatistics(String msg) {
			// overridden as no-op so that loggerModule can be left
			// unconfigured.
		}
	}

	public static class TestFrontier extends WorkQueueFrontier /*AbstractFrontier*/ {
		SortedMap<Integer, Queue<String>> inactiveQueuesByPrecedence
			= new TreeMap<Integer, Queue<String>>();
		Queue<String> retiredQueue = new ArrayDeque<String>();
		public TestFrontier() {
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

	@SuppressWarnings("serial")
	public static class TestWorkQueue extends WorkQueue {
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

	ConfigurableApplicationContext appCtx;
	protected CrawlController controller;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		appCtx = new AnnotationConfigApplicationContext(TestApp.class);
		controller = appCtx.getBean(CrawlController.class);
		// need to be configured here because of @Value("25") annotation.
		controller.setMaxToeThreads(5);
		// CrawlController.start() must be run in a thread in AlertThreadGroup, or
		// requestCrawlStart() will fail with NullPointerException in setupToePool()
		AlertThreadGroup atg = new AlertThreadGroup("test");
		Thread th = new Thread(atg, "launcher") {
			public void run() { appCtx.start(); }
		};
		th.start();
		try {
			th.join();
		} catch (InterruptedException ex) {
			fail("launch thread got interrupted");
		}

		Frontier frontier = appCtx.getBean(Frontier.class);
		CrawlURI curi = new CrawlURI(
				UURIFactory.getInstance("http://archive.org/"), "", null,
				null);
		frontier.schedule(curi);
	}

	@Override
	protected void tearDown() throws Exception {
		appCtx.stop();
		// stop() does not close BDB. we need to call destroy(), or
		// next test will hang trying to open a DB locked by previous
		// run.
		BdbModule bdb = appCtx.getBean(BdbModule.class);
		bdb.destroy();
		cleanUpOldFiles("bdb");
		super.tearDown();
	}

	protected String getReportOutput(Report report, CrawlController controller) throws IOException {
		// CrawlSummary report assumes there's at least one CrawlStatSnapshot, which can be
		// added by calling StatisticsTrakcer.run(), which in turn requires non-null
		// ApplicationContext and Frontier.
		StatisticsTracker stats = appCtx.getBean(StatisticsTracker.class);
		stats.run();

		StringWriter output = new StringWriter();
		PrintWriter writer = new PrintWriter(output);

		report.write(writer, stats);

		writer.flush();
		return output.toString();
	}

	public void testToeThreadsReport_WithoutToeThreads() throws Exception {
		Report report = new ToeThreadsReport();
		String out = getReportOutput(report, controller);
		assertEquals("no ToeThreads\n", out);
	}

	public void testToeThreadReport_WithToeThreads() throws Exception {
		controller.requestCrawlStart();

		Report report = new FreeMarkerReport("ToeThreads.ftl",
				"threads-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
		assertTrue(out != null && !out.equals(""));
	}

	public void testProcessorsReport_NoChains() throws Exception {
		Report report = new FreeMarkerReport("Processors.ftl",
				"processors-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	public void testProcessorsReport() throws Exception {
		Report report = new FreeMarkerReport("Processors.ftl",
				"processors-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	public void testFrontierSummaryReport() throws Exception {
		Report report = new FreeMarkerReport("FrontierSummary.ftl",
				"frontier-summary-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	public void testFrontierNonemptyReport() throws Exception {
		Report report = new FreeMarkerReport("FrontierNonempty.ftl",
				"forntier-nonempty-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);

		// some obvious checks
		assertFalse("output should not be 'frontier unstarted'",
				"frontier unstarted\n".equals(out));
	}

	public void testCrawlSummaryReport() throws Exception {
		Report report = new FreeMarkerReport("CrawlSummary.ftl", "crawl-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

	protected CrawlURI crawledURI(String url, String mimetype) throws URIException {
		CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
		curi.setContentType(mimetype);
		curi.setFetchStatus(200);
		curi.setContentSize(1000);
		return curi;
	}

	public void testMimetypesReport() throws Exception {
		// put some crawl statistics
		StatisticsTracker stats = appCtx.getBean(StatisticsTracker.class);
		stats.crawledURISuccessful(crawledURI("http://example.com/", "text/html"));
		stats.crawledURISuccessful(crawledURI("http://exmaple.com/logo.png", "image/png"));

		Report report = new FreeMarkerReport("Mimetypes.ftl", "mimetype-report.txt");
		String out = getReportOutput(report, controller);
		System.out.println(out);
	}

}
