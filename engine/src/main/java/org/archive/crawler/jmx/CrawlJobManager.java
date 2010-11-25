package org.archive.crawler.jmx;

import java.io.IOException;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.reporting.StatisticsTracker;

/**
 * MXBean implementation for managing {@link CrawlJob}.
 * @author kenji
 */
public class CrawlJobManager implements CrawlJobMXBean {
    private String name;
    private Engine engine;
    
    public CrawlJobManager(String name, Engine engine) {
        this.name = name;
        this.engine = engine;
    }
    
    protected CrawlJob getJob() {
        return engine.getJob(name);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.jmx.CrawlJobMXBean#isRunning()
     */
    public boolean isRunning() {
        CrawlJob j = getJob();
        if (j == null)
            return false;
        else
            return j.isRunning();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.jmx.CrawlJobMXBean#getRateReport()
     */
    public RateReport getRateReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new RateReport(j.rateReportData());
    }
    
    public LoadReport getLoadReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new LoadReport(j.loadReportData());
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.jmx.CrawlJobMXBean#getUriTotalsReport()
     */
    public UriTotalsReport getUriTotalsReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new UriTotalsReport(j.uriTotalsReportData());
    }
    
    public SizeTotalsReport getSizeTotalsReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new SizeTotalsReport(j.sizeTotalsReportData());
    }
    
    public ElapsedReport getElapsedReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new ElapsedReport(j.elapsedReportData());
    }
    
    public ThreadReport getThreadReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        return new ThreadReport(j.threadReportData());
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.jmx.CrawlJobMXBean#getFrontierReport()
     */
    public FrontierReport getFrontierReport() {
        CrawlJob j = getJob();
        if (j == null) return null;
        CrawlController c = j.getCrawlController();
        if (c == null) return null;
        return new FrontierReport(c.getFrontier());
    }
    
    @Override
    public long getLastActivityTime() throws IOException {
        CrawlJob j = getJob();
        if (j == null) return 0;
        return j.getLastActivityTime();
    }
    
    @Override
    public int getAlertCount() throws IOException {
        CrawlJob j = getJob();
        if (j == null) return 0;
        return j.getAlertCount();
    }
    
    @Override
    public String getJobStatusDescription() throws IOException {
        CrawlJob j = getJob();
        if (j == null) return null;
        return j.getJobStatusDescription();
    }
    
    @Override
    public CrawlStat getCrawlStat() throws IOException {
        CrawlJob j = getJob();
        if (j == null) return null;
        CrawlController controller = j.getCrawlController();
        if (controller == null) return null;
        return new CrawlStat(controller);
    }

    @Override
    public int getMaxToeThreads() throws IOException {
        CrawlJob j = getJob();
        if (j == null) return 0;
        CrawlController controller = j.getCrawlController();
        if (controller == null) {
            // XXX should throw more specific exception?
            throw new IOException("no controller");
        }
        return controller.getMaxToeThreads();
    }

    @Override
    public void setMaxToeThreads(int maxToeThreads) throws IOException {
        CrawlJob j = getJob();
        if (j == null) {
            throw new IOException("job \"" + name + "\" no longer exists");
        }
        CrawlController controller = j.getCrawlController();
        if (controller == null) {
            // XXX should throw more specific exception?
            throw new IOException("no controller");
        }
        controller.setMaxToeThreads(maxToeThreads);
    }
    
}
