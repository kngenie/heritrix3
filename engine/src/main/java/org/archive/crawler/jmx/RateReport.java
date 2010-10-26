package org.archive.crawler.jmx;

import java.util.Map;

public class RateReport extends ReportBean {
    private double currentDocsPerSecond;
    private double averageDocsPerSecond;
    private long currentKiBPerSec;
    private long averageKiBPerSec;
    
    public RateReport(Map<String, Number> map) {
        this.currentDocsPerSecond = getDouble(map, "currentDocsPerSecond");
        this.averageDocsPerSecond = getDouble(map, "averageDocsPerSecond");
        this.currentKiBPerSec = getLong(map, "currentKiBPerSec");
        this.averageKiBPerSec = getLong(map, "averageKiBPerSec");
    }
    public double getCurrentDocsPerSecond() {
        return currentDocsPerSecond;
    }
    public double getAverageDocsPerSecond() {
        return averageDocsPerSecond;
    }
    public long getCurrentKiBPerSec() {
        return currentKiBPerSec;
    }
    public long getAverageKiBPerSec() {
        return averageKiBPerSec;
    }
    
}
