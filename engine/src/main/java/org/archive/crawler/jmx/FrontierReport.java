package org.archive.crawler.jmx;

import java.util.Map;

import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Frontier;

/**
 * A <code>FrontierReport</code> holds {@link Frontier}-related statistics
 * from {@link CrawlJob#frontierReportData()}.
 * <p>{@link CrawlJobMXBean#getFrontierReport()} returns this object copying values from
 * a Map returned by {@link CrawlJob#frontierReportData()}, as it makes annotation on values
 * easier to do.</p>
 * <p>{@link CrawlJob#frontierReportData()} could return this object instead of a Map.</p>
 * @author kenji
 */
public class FrontierReport extends ReportBean {
    // these stats are from Frontier#shortReportMap() and
    // mostly Frontier implementation (WorkQueueFrontier) dependent.
    // so these fields should not be hard-coded, but currently they are. XXX
    private int totalQueues;
    private int inProcessQueues;
    private int readyQueues;
    private int snoozedQueues;
    private int activeQueues;
    private int inactiveQueues;
    private int ineligibleQueues;
    private int retiredQueues;
    private int exhaustedQueues;
    private int lastReachedState;
    // only meaningful in old Frontier
    private int inboundCount;
    private int outboundCount;
    
    // stats found in UriTotalsReport (minus totalUriCount)
    private long downloadedUriCount;
    private long queuedUriCount;
    private long futureUriCount;
    
    // stats found in CrawlStatSnapshot
    private long discoveredUriCount;
    private long finishedUriCount;
    private long downloadFailures;
    private long downloadDisregards;
    
    private float congestionRatio;
    private long deepestUri;
    private long averageDepth;
    
    public FrontierReport(Frontier frontier) {
        Map<String, Object> map = frontier.shortReportMap();
        this.totalQueues = getInt(map, "totalQueues");
        this.inProcessQueues = getInt(map, "inProcessQueues");
        this.readyQueues = getInt(map, "readyQueues");
        this.snoozedQueues = getInt(map, "snoozedQueues");
        this.activeQueues = getInt(map, "activeQueues");
        this.inactiveQueues = getInt(map, "inactiveQueues");
        this.ineligibleQueues = getInt(map, "ineligibleQueues");
        this.retiredQueues = getInt(map, "retiredQueues");
        this.exhaustedQueues = getInt(map, "exhaustedQueues");
        this.lastReachedState = getInt(map, "lastReachedState");
        this.inboundCount = getInt(map, "inboundCount");
        this.outboundCount = getInt(map, "outboundCount");
        
        this.downloadedUriCount = frontier.succeededFetchCount();
        this.queuedUriCount = frontier.queuedUriCount();
        this.futureUriCount = frontier.futureUriCount();
        
        this.discoveredUriCount = frontier.discoveredUriCount();
        this.finishedUriCount = frontier.finishedUriCount();
        this.downloadFailures = frontier.failedFetchCount();
        this.downloadDisregards = frontier.disregardedUriCount();
        
        this.congestionRatio = frontier.congestionRatio();
        this.deepestUri = frontier.deepestUri();
        this.averageDepth = frontier.averageDepth();        
    }
    public int getTotalQueues() {
        return totalQueues;
    }
    public int getInProcessQueues() {
        return inProcessQueues;
    }
    public int getReadyQueues() {
        return readyQueues;
    }
    public int getSnoozedQueues() {
        return snoozedQueues;
    }
    public int getActiveQueues() {
        return activeQueues;
    }
    public int getInactiveQueues() {
        return inactiveQueues;
    }
    public int getIneligibleQueues() {
        return ineligibleQueues;
    }
    public int getRetiredQueues() {
        return retiredQueues;
    }
    public int getExhaustedQueues() {
        return exhaustedQueues;
    }
    public int getLastReachedState() {
        return lastReachedState;
    }
    public int getInboundCount() {
        return inboundCount;
    }
    public int getOutboundCount() {
        return outboundCount;
    }
    /**
     * number of URIs successfully fetched.
     * @return non-negative long.
     */
    public long getDownloadedUriCount() {
        return downloadedUriCount;
    }
    public long getQueuedUriCount() {
        return queuedUriCount;
    }
    public long getFutureUriCount() {
        return futureUriCount;
    }
    public long getDiscoveredUriCount() {
        return discoveredUriCount;
    }
    public long getFinishedUriCount() {
        return finishedUriCount;
    }
    public long getDownloadFailures() {
        return downloadFailures;
    }
    public long getDownloadDisregards() {
        return downloadDisregards;
    }
    public float getCongestionRatio() {
        return congestionRatio;
    }
    public long getDeepestUri() {
        return deepestUri;
    }
    public long getAverageDepth() {
        return averageDepth;
    }
    
}
