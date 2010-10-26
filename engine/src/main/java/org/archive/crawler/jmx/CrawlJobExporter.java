package org.archive.crawler.jmx;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Engine;
/**
 * a class that exposes {@link Engine} through JMX. Since {@link Engine} itself has little to expose,
 * this class exposes {@link CrawlJob}s under its management directly as objects, and reflects changes
 * over time.
 *  
 * @contributor kenji <kenji@archive.org>
 */
public class CrawlJobExporter {
    private static Logger LOGGER = Logger.getLogger(CrawlJobExporter.class.getName());
    protected Engine engine;
    protected Map<String, CrawlJobManager> exportedJobs;
    
    /**
     * initializes CrawlJobExporter. Keeps reference to <code>engine</code>,
     * for later calls on {@link #jobsChanged()}. This constructor does not
     * export {@link CrawlJob}s. You need to call {@link #jobsChanged()} soon
     * after creating CrawlJobExporter.
     * @param engine {@link Engine}
     */
    public CrawlJobExporter(Engine engine) {
        this.engine = engine;
        this.exportedJobs = new HashMap<String, CrawlJobManager>();
        //exportJobs(engine.getJobConfigs());
    }
    private void exportJobs(Map<String, CrawlJob> jobConfigs) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        
        Set<String> names = new HashSet<String>(exportedJobs.keySet());
        for (Entry<String, CrawlJob> ent : jobConfigs.entrySet()) {
            if (exportedJobs.containsKey(ent.getKey())) {
                // already exported and continues to be exported.
                names.remove(ent.getKey());
            } else {
                // new job
                CrawlJobManager adapter = new CrawlJobManager(ent.getKey(), engine);
                String objectNameText = "org.archive.crawler:type=CrawlJob,name=" + ent.getKey();
                try {
                    ObjectName objectName = new ObjectName(objectNameText);
                    mbs.registerMBean(adapter, objectName);
                    exportedJobs.put(ent.getKey(), adapter);
                } catch (JMException ex) {
                    LOGGER.warning("failed to register crawl job " + ent.getKey() +
                            " by ObjectName:" + objectNameText + "(" + ex.getMessage() + ")");
                }
            }
        }
        // unregister those disappeared
        for (String name : names) {
            String objectNameText = "org.archive.crawler:type=CrawlJob,name=" + name;
            try {
                mbs.unregisterMBean(new ObjectName(objectNameText));
            } catch (Exception ex) {
                LOGGER.warning("error during unregisterMBean " + objectNameText + "(" + ex.getMessage() + ")");
                // already unregistered. okay to ignore the error.
            }
        }
    }
    /**
     * run this method whenever job(s) created/removed.
     * TODO this method would become part of "JobsListener" interface.
     */
    public void jobsChanged() {
        Map<String, CrawlJob> jobs = engine.getJobConfigs();
        exportJobs(jobs);
    }
}
