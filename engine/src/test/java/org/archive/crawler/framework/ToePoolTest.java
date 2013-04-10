/**
 *
 */
package org.archive.crawler.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import junit.framework.TestCase;

import org.archive.crawler.frontier.AbstractFrontier;
import org.archive.crawler.reporting.AlertThreadGroup;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;

/**
 * @author kenji
 *
 */
public class ToePoolTest extends TestCase {
	public class TestCrawlController extends CrawlController {
		@Override
		public CrawlMetadata getMetadata() {
			CrawlMetadata metadata = new CrawlMetadata();
			metadata.setJobName("test");
			return metadata;
		}
	}
	public class TestFrontier extends AbstractFrontier {
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
			return null;
		}
		@Override
		public void reportTo(PrintWriter writer) throws IOException {
			// TODO Auto-generated method stub

		}
		@Override
		public String shortReportLegend() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public void shortReportLineTo(PrintWriter pw) throws IOException {
			// TODO Auto-generated method stub

		}
		@Override
		public Map<String, Object> shortReportMap() {
			// TODO Auto-generated method stub
			return null;
		}
		//@Override
		public Map<String, Object> reportMap() {
			return null;
		}
		@Override
		protected void processScheduleAlways(CrawlURI caUri) {
		}
		@Override
		protected void processScheduleIfUnique(CrawlURI caUri) {
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
		//@Override
		public long candidateUriCount() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	ToePool testobj;
	@Override
	protected void setUp() throws Exception {
		AlertThreadGroup atg = new AlertThreadGroup("alert");
		TestCrawlController crawlController = new TestCrawlController();
		crawlController.setFrontier(new TestFrontier());
		testobj = new ToePool(atg, crawlController);
	}
	public void testShortReportLineTo() {
		testobj.setSize(10);
		StringWriter w = new StringWriter();
		//testobj.shortReportLineTo(new PrintWriter(w));
		System.err.println(w.toString());
	}
}
