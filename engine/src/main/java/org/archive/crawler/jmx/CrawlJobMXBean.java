package org.archive.crawler.jmx;

import java.io.IOException;

import org.archive.crawler.framework.CrawlJob;

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

    public abstract RateReport getRateReport() throws IOException;

    public abstract UriTotalsReport getUriTotalsReport() throws IOException;

    public abstract FrontierReport getFrontierReport() throws IOException;

    public abstract LoadReport getLoadReport() throws IOException;

    public abstract SizeTotalsReport getSizeTotalsReport() throws IOException;

    public abstract ElapsedReport getElapsedReport() throws IOException;

    public ThreadReport getThreadReport() throws IOException;

}
