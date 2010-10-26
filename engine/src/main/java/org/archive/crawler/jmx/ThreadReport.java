package org.archive.crawler.jmx;

import java.util.Map;

public class ThreadReport extends ReportBean {
    private long toeCount;
    private long steps;
    private long processors;
    public ThreadReport(Map<String, ?> map) {
        this.toeCount = getLong(map, "toeCount");
        this.steps = getLong(map, "steps");
        this.processors = getLong(map, "processors");
    }
    
    public long getToeCount() {
        return toeCount;
    }
    public long getSteps() {
        return steps;
    }
    public long getProcessors() {
        return processors;
    }
    
}
