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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.FetchStatusCodes;
import org.archive.modules.hq.HttpHeadquarterAdapter;
import org.archive.modules.recrawl.PersistProcessor;
import org.archive.modules.recrawl.RecrawlAttributeConstants;

/**
 * @contributor kenji
 */
public class HBasePersistStoreProcessor extends HBasePersistProcessor implements FetchStatusCodes, RecrawlAttributeConstants {
    private static final Logger logger = Logger.getLogger(HttpHeadquarterAdapter.class.getName());

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

    private static String getHeaderValue(org.apache.commons.httpclient.HttpMethod method, String name) {
        org.apache.commons.httpclient.Header header = method.getResponseHeader(name);
        return header != null ? header.getValue() : null;
    }
    
    protected static final DateFormat HTTP_DATE_FORMAT = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    
    /**
     * converts time in HTTP Date format {@code dateStr} to seconds
     * since epoch. 
     * @param dateStr time in HTTP Date format.
     * @return seconds since epoch
     */
    protected long parseHttpDate(String dateStr) {
        synchronized (HTTP_DATE_FORMAT) {
            try {
                Date d = HTTP_DATE_FORMAT.parse(dateStr);
                return d.getTime() / 1000;
            } catch (ParseException ex) {
                if (logger.isLoggable(Level.FINER))
                    logger.fine("bad HTTP DATE: " + dateStr);
                return 0;
            }
        }
    }
    
    protected Put createFinishedRequest(CrawlURI uri) {
        byte[] uriBytes = Bytes.toBytes(uri.toString());
        byte[] key = uriBytes;
        Put p = new Put(key);
        //p.add(COLUMN_FAMILY, Bytes.toBytes("u"), uriBytes);
        // reading history data directly from CrawlURI fields, instead of FETCH_HISTORY
        // data. this spares FetchHistoryProcessor.
        String digest = uri.getContentDigestSchemeString();
        if (digest != null) {
            p.add(COLUMN_FAMILY, COLUMN_CONTENT_DIGEST, Bytes.toBytes(digest));
        }
        p.add(COLUMN_FAMILY, COLUMN_STATUS, Bytes.toBytes(Integer.toString(uri.getFetchStatus())));
        org.apache.commons.httpclient.HttpMethod method = uri.getHttpMethod();
        if (method != null) {
            String etag = getHeaderValue(method, RecrawlAttributeConstants.A_ETAG_HEADER);
            if (etag != null) {
                // Etqg is usually quoted
                if (etag.length() >= 2 && etag.charAt(0) == '"' && etag.charAt(etag.length() - 1) == '"')
                    etag = etag.substring(1, etag.length() - 1);
                p.add(COLUMN_FAMILY, COLUMN_ETAG, Bytes.toBytes(etag));
            }
            String lastmod = getHeaderValue(method, RecrawlAttributeConstants.A_LAST_MODIFIED_HEADER);
            if (lastmod != null) {
                long lastmod_sec = parseHttpDate(lastmod);
                if (lastmod_sec == 0) {
                    try {
                        lastmod_sec = uri.getFetchCompletedTime();
                    } catch (NullPointerException ex) {
                        logger.warning("CrawlURI.getFetchCompletedTime():" + ex + " for " + uri.shortReportLine());
                    }
                }
                if (lastmod_sec != 0)
                    p.add(COLUMN_FAMILY, COLUMN_LAST_MODIFIED, Bytes.toBytes(lastmod_sec));
            } else {
                try {
                    long completed = uri.getFetchCompletedTime();
                    if (completed != 0)
                        p.add(COLUMN_FAMILY, COLUMN_LAST_MODIFIED, Bytes.toBytes(completed));
                } catch (NullPointerException ex) {
                    logger.warning("CrawlURI.getFetchCompletedTime():" + ex + " for " + uri.shortReportLine());
                }
            }
        }
        return p;
    }
    
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
        // prerequisite URIs such as DNS need not be persisted
        // TODO: we want to deduplicate robots.txt, too
//        if (uri.isPrerequisite()) return false;
        String scheme = uri.getUURI().getScheme();
        if (!(scheme.equals("http") || scheme.equals("https"))) return false;
        //if (uri.getHttpMethod() == null) return false;
        // && curi.isSuccess();
        if (getOnlyStoreIfWriteTagPresent() && !hasWriteTag(uri)) return false;
        if (!uri.isSuccess()) return false;
//        int fetchStatus = uri.getFetchStatus();
//        if (fetchStatus == S_RUNTIME_EXCEPTION ||
//                fetchStatus == S_SERIOUS_ERROR ||
//                fetchStatus == S_PROCESSING_THREAD_KILLED ||
//                fetchStatus == S_BLOCKED_BY_RUNTIME_LIMIT)
//            return false;
        return true;
    }
    
    @Override
    protected void innerProcess(CrawlURI uri) {
        Put p = createFinishedRequest(uri);
        try {
            client.put(p);
        } catch (IOException ex) {
            logger.warning(uri + " not stored due to an error: " + ex);
        }
    }
}
