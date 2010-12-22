package org.archive.crawler.jmx;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.reporting.CrawlStatSnapshot;
import org.archive.crawler.reporting.StatisticsTracker;

/**
 * CrawlStat wraps around {@link CrawlStatSnapshot} and exposes
 * JavaBean compliant getters for those stats and also hides unwanted methods.
 * <p>TODO I'm dropping this class in favor of more direct {@link FrontierReport} and others, because
 * CrawlStat relies on {@link StatisticsTracker} and {@link CrawlStatSnapshot}, that implement H3's own
 * stat tracking mechanism. As JMX is meant for use with external stat tracking mechanism, H3 JMX interface
 * should not rely on those classes. This class is kept as extension of {@link FrontierReport} to
 * support existing monitoring code expecting this interface to exist. Now properties in this class are
 * either not useful for monitoring through JMX, or are useful and only available through {@link StatisticsTracker}.</p>
 * <p>TODO byteProcessed and elapsedMilliseconds
 * are only found in {@link StatisticsTracker} and still useful. We need to find a new
 * home for these stats before entirely dropping this class, probably {@link Frontier}.</p>
 * @see CrawlJobMXBean
 * @see CrawlJobManager
 * @contributor Kenji Nagahashi
 */
public class CrawlStat extends FrontierReport {
    private CrawlController controller;
    private CrawlStatSnapshot snapshot;
//    private StatisticsTracker stats;
    public CrawlStat(CrawlController controller) {
        super(controller.getFrontier());
        this.controller = controller;
//        this.stats = controller.getStatisticsTracker();
//        StatisticsTracker stats = controller.getStatisticsTracker();
//        this.snapshot = stats.getSnapshot();
//        this.totalThreads = stats.threadCount();
    }
    /**
     * lazily obtain {@link CrawlStatSnapshot} object
     * @return
     */
    protected CrawlStatSnapshot getSnapshot() {
        if (snapshot == null)
            snapshot = controller.getStatisticsTracker().getSnapshot();
        return snapshot;
    }
    // dropped because nobody sets CrawlStatSnapshot#urisFetched
//    public long getUrisFetched() {
//        return snapshot.urisFetched;
//    }
    /**
     * amounts of data downloaded.
     * @return bytes
     */
    public long getBytesProcessed() {
//        return snapshot.bytesProcessed;
        return controller.getStatisticsTracker().getCrawledBytes().getTotalBytes();
    }
    /**
     * sum of {@link #getQueuedUriCount()}, {@link #busyThreads()} and
     * {@link #getDownloadedUriCount()}.
     * @return
     */
    public long getTotalUriCount() {
        //return getSnapshot().totalCount();
        // XXX duplicating CrawlStatSnapshot#totalCount()
        return getQueuedUriCount() + getBusyThreads() + getDownloadedUriCount();
    }
    /**
     * time spent on crawling.
     * @return milli seconds
     */
    public long getElapsedMilliseconds() {
        return controller.getStatisticsTracker().getCrawlElapsedTime();
    }
    /**
     * this number is average over whole uptime, derived from
     * downloadUriCount and crawlElapsedTime.
     * @return double pages per second
     */
    public double getDocsPerSecond() {
        return getSnapshot().docsPerSecond;
    }
    /**
     * this is average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double pages per second
     */
    public double getCurrentDocsPerSecond() {
        return getSnapshot().currentDocsPerSecond;
    }
    /**
     * derived from bytesProcessed and crawlElapsedTime.
     * @return double kilo bytes per second
     */
    public double getTotalKiBPerSec() {
        return getSnapshot().totalKiBPerSec;
    }
    /**
     * this is an average since the last sample.
     * unreliable as statistics - maybe dropped soon.
     * @return double kilo bytes per second
     */
    public double getCurrentKiBPerSec() {
        return getSnapshot().currentKiBPerSec;
    }
    /**
     * number of active toe threads.
     * @return non-negative integer
     */
    public int getBusyThreads() {
        return controller.getActiveToeCount();
    }
    public int getTotalThreads() {
//        return controller.getStatisticsTracker().threadCount();
        return controller.getToeCount();
    }
}
