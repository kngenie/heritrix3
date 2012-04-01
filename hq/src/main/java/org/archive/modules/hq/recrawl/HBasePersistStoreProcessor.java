/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.modules.hq.recrawl;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.hadoop.hbase.client.Put;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.FetchStatusCodes;
import org.archive.modules.recrawl.PersistProcessor;
import org.archive.modules.recrawl.RecrawlAttributeConstants;

/**
 * @contributor kenji
 */
public class HBasePersistStoreProcessor extends HBasePersistProcessor implements FetchStatusCodes, RecrawlAttributeConstants {
    private static final Logger logger = Logger.getLogger(HBasePersistStoreProcessor.class.getName());

    public HBasePersistStoreProcessor() {
    }
    
    /**
     * following three members are duplicates of {@link PersistProcessor} members.
     * HBasePersistStoreProcessor cannot be a sub-class of PersistProcessor because
     * it is dependent on BDB. Probably we should generalize PersistProcessor into
     * a base-class reusable for different storage means.
     */
    boolean onlyStoreIfWriteTagPresent = true;
    public boolean getOnlyStoreIfWriteTagPresent() {
        return onlyStoreIfWriteTagPresent;
    }
    public void setOnlyStoreIfWriteTagPresent(boolean onlyStoreIfWriteTagPresent) {
        this.onlyStoreIfWriteTagPresent = onlyStoreIfWriteTagPresent;
    }
    // end duplicates

    /**
     * test if {@code uri} has WRITE_TAG in the latest fetch history (i.e. this crawl).
     * this code is a duplicate of {@link PersistProcessor#shouldProcess}.
     * @param uri
     * @return
     */
    @SuppressWarnings("unchecked")
    protected boolean hasWriteTag(CrawlURI uri) {
        Map<String,Object>[] history = (Map<String,Object>[])uri.getData().get(A_FETCH_HISTORY);
        return history != null && history[0] != null && history[0].containsKey(A_WRITE_TAG);
    }
    
    @Override
    protected boolean shouldProcess(CrawlURI uri) {
        // do this first for quick decision on CURLs postponed by prerequisite
        if (!uri.isSuccess()) return false;
        // DNS query need not be persisted
        String scheme = uri.getUURI().getScheme();
        if (!(scheme.equals("http") || scheme.equals("https"))) return false;
        if (getOnlyStoreIfWriteTagPresent() && !hasWriteTag(uri)) return false;
        return true;
    }
    
    public static final int PUT_RETRY_INTERVAL_MS = 10*1000;
    
    @Override
    protected void innerProcess(CrawlURI uri) {
        Put p = schema.createPut(uri);
        int retry = 0;
        do {
            try {
                client.put(p);
                break;
            } catch (IOException ex) {
                logger.warning(uri + " put for " + uri + " failed" + 
                        (retry > 0 ? "(retry " + retry + ")" : "") + ": " + ex);
            }
            retry++;
            try {
                Thread.sleep(PUT_RETRY_INTERVAL_MS);
            } catch (InterruptedException ex) {
                logger.warning("thread interrupted. aborting retry for " + uri);
                break;
            }
        } while (isRunning());
    }
}
