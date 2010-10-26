package org.archive.crawler.jmx;

import java.util.Map;

public class LoadReport extends ReportBean {
    private int busyThreads;
    private int totalThreads;
    private double congestionRatio;
    private long averageQueueDepth;
    private long deepestQueueDepth;
    
    public LoadReport(Map<String, ?> map) {
        this.busyThreads = getInt(map, "busyThreads");
        this.totalThreads = getInt(map, "totalThreads");
        this.congestionRatio = getDouble(map, "congestionRatio");
        this.averageQueueDepth = getLong(map, "averageQueueDepth");
        this.deepestQueueDepth = getLong(map, "deepestQueueDepth");
    }

    public int getBusyThreads() {
        return busyThreads;
    }

    public int getTotalThreads() {
        return totalThreads;
    }

    public double getCongestionRatio() {
        return congestionRatio;
    }

    public long getAverageQueueDepth() {
        return averageQueueDepth;
    }

    public long getDeepestQueueDepth() {
        return deepestQueueDepth;
    }
    
}
