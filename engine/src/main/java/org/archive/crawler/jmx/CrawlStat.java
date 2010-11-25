package org.archive.crawler.jmx;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.reporting.CrawlStatSnapshot;
import org.archive.crawler.reporting.StatisticsTracker;

/**
 * CrawlStat is a JavaBean compliant wrapper around {@link CrawlStatSnapshot}, which exposes
 * getters for those stats and hides unwanted methods.
 * <p>TODO I'm dropping this class in favor of more direct {@link FrontierReport} and others, because
 * CrawlStat replies on {@link StatisticsTracker} and {@link CrawlStatSnapshot}, that implement H3's own
 * stat tracking mechanism. As JMX is meant for use with external stat tracking mechanism, H3 JMX interface
 * should not rely on those classes. This class is kept as extension of {@link FrontierReport} to
 * support existing monitoring code using this interface.</p>
 * <p>TODO urisFetched, byteProcessed, elapsedMilliseconds, busyThreads, and
 * totalThreads are only found in this class and still useful. We need to find a new
 * home for these stats before entirly dropping this class.</p>
 * @see CrawlJobMXBean
 * @see CrawlJobManager
 * @contributor Kenji Nagahashi
 */
public class CrawlStat extends FrontierReport {
    private CrawlStatSnapshot snapshot;
    private int totalThreads;
    public CrawlStat(CrawlController controller) {
        super(controller.getFrontier());
        StatisticsTracker stats = controller.getStatisticsTracker();
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
}
