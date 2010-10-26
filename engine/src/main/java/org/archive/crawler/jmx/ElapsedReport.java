package org.archive.crawler.jmx;

import java.util.Map;

import org.archive.crawler.framework.CrawlJob;

/**
 * statistics report bean for use with a Map returned by {@link CrawlJob#elapsedReportData()}.
 * @see FrontierReport
 * @contributor kenji
 */
public class ElapsedReport extends ReportBean {
    private long elapsedMilliseconds;
    private String elapsedPretty;
    /**
     * initializes member fields from <code>map</code>.
     * @param map Map returned by {@link CrawlJob#elapsedReportData()}.
     */
    public ElapsedReport(Map<String, ?> map) {
        this.elapsedMilliseconds = getLong(map, "elapsedMilliseconds");
        this.elapsedPretty = getString(map, "elapsedPretty");
    }
    public long getElapsedMilliseconds() {
        return elapsedMilliseconds;
    }
    public String getElapsedPretty() {
        return elapsedPretty;
    }
    
}
