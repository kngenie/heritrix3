package org.archive.crawler.jmx;

import org.archive.crawler.reporting.CrawlStatSnapshot;

/**
 * CrawlStat is a JavaBean compliant wrapper around {@link CrawlStatSnapshot}, which exposes
 * getters for those stats and hides unwanted methods.
 * <p>TODO it would be more efficient for {@link CrawlJobManager} to create CompositeData directly
 * rather than relying on JMX framework to do it.</p>
 * @see CrawlJobMXBean
 * @see CrawlJobManager
 * @contributor Kenji Nagahashi
 */
public class CrawlStat {
    private CrawlStatSnapshot stat;
    public CrawlStat(CrawlStatSnapshot stat) {
        this.stat = stat;
    }
    /**
     * time this snapshot was taken.
     * @return {@link System#currentTimeMillis()} value.
     */
    public long getTimestamp() {
        return stat.timestamp;
    }
    public long getUrisFetched() {
        return stat.urisFetched;
    }
    /**
     * amounts of data downloaded.
     * @return bytes
     */
    public long getBytesProcessed() {
        return stat.bytesProcessed;
    }
    public long getDiscoveredUriCount() {
        return stat.discoveredUriCount;
    }
    public long getQueuedUriCount() {
        return stat.queuedUriCount;
    }
    public long getFutureUriCount() {
        return stat.futureUriCount;
    }
    public long getFinishedUriCount() {
        return stat.finishedUriCount;
    }
    /**
     * number of URIs successfully fetched.
     * @return non-negative long.
     */
    public long getDownloadedUriCount() {
        return stat.downloadedUriCount;
    }
    public long getDownloadFailures() {
        return stat.downloadFailures;
    }
    public long getDownloadDisregards() {
        return stat.downloadDisregards;
    }
    /**
     * time spent on crawling.
     * @return milli seconds
     */
    public long getElapsedMilliseconds() {
        return stat.elapsedMilliseconds;
    }
    /**
     * this number is average over whole uptime, derived from
     * downloadUriCount and crawlElapsedTime.
     * @return double pages per second
     */
    public double getDocsPerSecond() {
        return stat.docsPerSecond;
    }
    /**
     * this is average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double pages per second
     */
    public double getCurrentDocsPerSecond() {
        return stat.currentDocsPerSecond;
    }
    /**
     * derived from bytesProcessed and crawlElapsedTime.
     * @return double kilo bytes per second
     */
    public double getTotalKiBPerSec() {
        return stat.totalKiBPerSec;
    }
    /**
     * this is an average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double kilo bytes per second
     */
    public double getCurrentKiBPerSec() {
        return stat.currentKiBPerSec;
    }
    /**
     * number of active toe threads.
     * @return non-negative integer
     */
    public int busyThreads() {
        return stat.busyThreads;
    }
    public float getCongestionRatio() {
        return stat.congestionRatio;
    }
    public long getDeepestUri() {
        return stat.deepestUri;
    }
    public long getAverageDepth() {
        return stat.averageDepth;
    }
}
