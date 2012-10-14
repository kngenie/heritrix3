package org.archive.crawler.reporting;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * common super-class for {@link Report}s on {@link StatisticsTracker}.
 * @contributor Kenji Nagahashi
 */
public abstract class StatisticsReport extends Report {

    protected StatisticsTracker stats;

    public StatisticsTracker getStatisticsTracker() {
        return stats;
    }
    @Autowired
    public void setStatisticsTracker(StatisticsTracker stats) {
        this.stats = stats;
    }

}
