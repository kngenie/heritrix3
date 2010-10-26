package org.archive.crawler.jmx;

import java.util.Map;

import org.archive.crawler.util.CrawledBytesHistotable;

public class SizeTotalsReport extends ReportBean {
    private long notModified;
    private long dupByHash;
    private long novel;
    private long notModifiedCount;
    private long dupByHashCount;
    private long novelCount;
    
    public SizeTotalsReport(Map<String, Long> map) {
        this.notModified = getLong(map, CrawledBytesHistotable.NOTMODIFIED);
        this.dupByHash = getLong(map, CrawledBytesHistotable.DUPLICATE);
        this.novel = getLong(map, CrawledBytesHistotable.NOVEL);
        this.notModifiedCount = getLong(map, CrawledBytesHistotable.NOTMODIFIEDCOUNT);
        this.dupByHashCount = getLong(map, CrawledBytesHistotable.DUPLICATECOUNT);
        this.novelCount = getLong(map, CrawledBytesHistotable.NOVELCOUNT);
    }

    public long getNotModified() {
        return notModified;
    }

    public long getDupByHash() {
        return dupByHash;
    }

    public long getNovel() {
        return novel;
    }

    public long getNotModifiedCount() {
        return notModifiedCount;
    }

    public long getDupByHashCount() {
        return dupByHashCount;
    }

    public long getNovelCount() {
        return novelCount;
    }
    
}
