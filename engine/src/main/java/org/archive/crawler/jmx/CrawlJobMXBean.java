package org.archive.crawler.jmx;

import java.io.IOException;

import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.reporting.CrawlStatSnapshot;

/**
 * MXBean interface for offering statistics of crawl jobs.
 * TODO Standard MBean with Open MBean types would be more efficient considering
 * that {@link CrawlJob} report methods returns map.
 * @see CrawlJob
 * @see CrawljobExporter
 * @see FrontierReport
 * @author kenji
 */
public interface CrawlJobMXBean {

    public abstract boolean isRunning();

    /**
     * @deprecated same information available in {@link #getCrawlStat()}.
     * @return rate statistics in {@link RateReport}
     * @throws IOException
     */
    public abstract RateReport getRateReport() throws IOException;
    /**
     * @deprecated same information available in {@link #getCrawlStat()}
     * @return URI processing statistics in {@link UriTotalsReport}.
     * @throws IOException
     */
    public abstract UriTotalsReport getUriTotalsReport() throws IOException;

    public abstract FrontierReport getFrontierReport() throws IOException;

    /**
     * @deprecated same information available in {@link #getCrawlStat()}
     * @return processing load statistics in {@link LoadReport}
     * @throws IOException
     */
    public abstract LoadReport getLoadReport() throws IOException;

    public abstract SizeTotalsReport getSizeTotalsReport() throws IOException;

    public abstract ElapsedReport getElapsedReport() throws IOException;

    public ThreadReport getThreadReport() throws IOException;
    
    public String getJobStatusDescription() throws IOException;
    
    public int getAlertCount() throws IOException;
    
    public long getLastActivityTime() throws IOException;

    public CrawlStat getCrawlStat() throws IOException;
    
    public int getMaxToeThreads() throws IOException;
    
    public void setMaxToeThreads(int maxToeThreads) throws IOException;
}
