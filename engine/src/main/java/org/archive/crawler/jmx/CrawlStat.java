package org.archive.crawler.jmx;

import org.archive.crawler.reporting.CrawlStatSnapshot;
import org.archive.crawler.reporting.StatisticsTracker;

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
    private CrawlStatSnapshot snapshot;
    private int totalThreads;
    public CrawlStat(StatisticsTracker stats) {
        this.snapshot = stats.getSnapshot();
        this.totalThreads = stats.threadCount();
    }
    /**
     * time this snapshot was taken.
     * @return {@link System#currentTimeMillis()} value.
     */
    public long getTimestamp() {
        return snapshot.timestamp;
    }
    public long getUrisFetched() {
        return snapshot.urisFetched;
    }
    /**
     * amounts of data downloaded.
     * @return bytes
     */
    public long getBytesProcessed() {
        return snapshot.bytesProcessed;
    }
    public long getDiscoveredUriCount() {
        return snapshot.discoveredUriCount;
    }
    public long getQueuedUriCount() {
        return snapshot.queuedUriCount;
    }
    public long getFutureUriCount() {
        return snapshot.futureUriCount;
    }
    public long getFinishedUriCount() {
        return snapshot.finishedUriCount;
    }
    /**
     * number of URIs successfully fetched.
     * @return non-negative long.
     */
    public long getDownloadedUriCount() {
        return snapshot.downloadedUriCount;
    }
    public long getDownloadFailures() {
        return snapshot.downloadFailures;
    }
    public long getDownloadDisregards() {
        return snapshot.downloadDisregards;
    }
    /**
     * sum of {@link #getQueuedUriCount()}, {@link #busyThreads()} and
     * {@link #getDownloadedUriCount()}.
     * @return
     */
    public long getTotalUriCount() {
        return snapshot.totalCount();
    }
    /**
     * time spent on crawling.
     * @return milli seconds
     */
    public long getElapsedMilliseconds() {
        return snapshot.elapsedMilliseconds;
    }
    /**
     * this number is average over whole uptime, derived from
     * downloadUriCount and crawlElapsedTime.
     * @return double pages per second
     */
    public double getDocsPerSecond() {
        return snapshot.docsPerSecond;
    }
    /**
     * this is average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double pages per second
     */
    public double getCurrentDocsPerSecond() {
        return snapshot.currentDocsPerSecond;
    }
    /**
     * derived from bytesProcessed and crawlElapsedTime.
     * @return double kilo bytes per second
     */
    public double getTotalKiBPerSec() {
        return snapshot.totalKiBPerSec;
    }
    /**
     * this is an average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double kilo bytes per second
     */
    public double getCurrentKiBPerSec() {
        return snapshot.currentKiBPerSec;
    }
    /**
     * number of active toe threads.
     * @return non-negative integer
     */
    public int busyThreads() {
        return snapshot.busyThreads;
    }
    public int totalThreads() {
        return totalThreads;
    }
    public float getCongestionRatio() {
        return snapshot.congestionRatio;
    }
    public long getDeepestUri() {
        return snapshot.deepestUri;
    }
    public long getAverageDepth() {
        return snapshot.averageDepth;
    }
}
