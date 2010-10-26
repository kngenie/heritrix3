package org.archive.crawler.jmx;

import java.util.Map;

import org.archive.crawler.framework.CrawlJob;

/**
 * <code>UriTotalsReport</code> is subset of {@link org.archive.crawler.reporting.CrawlStatSnapshot}
 * that {@link CrawlJob#uriTotalsReportData()} provides.
 * <p>this is unnecessary if CrawlJob#getStats() were publicly accessible.</p>
 * 
 * @author kenji
 */
public class UriTotalsReport extends ReportBean {
    private long downloadedUriCount;
    private long queuedUriCount;
    private long totalUriCount;
    private long futureUriCount;
    
    /**
     * Constructor initializing from Map returned by {@link CrawlJob#uriTotalsReportData()}.
     * 
     * @param map
     */
    public UriTotalsReport(Map<String, Long> map) {
        this.downloadedUriCount = getLong(map, "downloadedUriCount");
        this.queuedUriCount = getLong(map, "queuedUriCount");
        this.totalUriCount = getLong(map, "totalUriCount");
        this.futureUriCount = getLong(map, "futureUriCount");
    }
    public long getDownloadedUriCount() {
        return downloadedUriCount;
    }
    public long getQueuedUriCount() {
        return queuedUriCount;
    }
    public long getTotalUriCount() {
        return totalUriCount;
    }
    public long getFutureUriCount() {
        return futureUriCount;
    }
    
}
