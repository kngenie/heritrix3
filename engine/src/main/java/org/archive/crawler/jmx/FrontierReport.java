package org.archive.crawler.jmx;

import java.lang.reflect.Field;
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
    private int inboundCount;
    private int outboundCount;
    
    public FrontierReport(Map<String, Object> map) {
        // copy values to fields with reflection. requires field names to match
        // map keys. to support cases with some report maps where keys have hyphens,
        // I'd need to implement an annotation class. 
        Field[] fields = getClass().getDeclaredFields();
        for (Field f : fields) {
            String name = f.getName();
            try {
                f.setInt(this, getInt(map, name));
            } catch (Exception ex) {
            }
        }
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
    
}
