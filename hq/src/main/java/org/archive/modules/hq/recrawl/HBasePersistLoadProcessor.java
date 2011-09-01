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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.modules.recrawl.PersistLoadProcessor;
import org.archive.modules.recrawl.RecrawlAttributeConstants;

/**
 * A {@link Processor} for retrieving recrawl info from HBase table.
 * See {@link HBasePersistProcessor} for table schema.
 * @see HBasePersistStoreProcessor
 * @contributor kenji
 */
public class HBasePersistLoadProcessor extends HBasePersistProcessor {
    private static final Logger logger =
        Logger.getLogger(PersistLoadProcessor.class.getName());

    protected static final DateFormat HTTP_DATE_FORMAT = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    protected String formatHttpDate(long time) {
        synchronized (HTTP_DATE_FORMAT) {
            // format is not thread safe either
            return HTTP_DATE_FORMAT.format(new Date(time * 1000));
        }
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object>[] getFetchHistory(CrawlURI uri) {
        Map<String, Object> data = uri.getData();
        Map<String, Object>[] history = (Map[])data.get(RecrawlAttributeConstants.A_FETCH_HISTORY);
        if (history == null) {
            // only the first element is used by FetchHTTP, WarcWriterProcessor etc.
            // FetchHistoryProcessor casts history to HashMap[]. So it must be new HashMap[1].
            history = new HashMap[2];
            data.put(RecrawlAttributeConstants.A_FETCH_HISTORY, history);
        }
        return history;
    }
    
    @Override
    protected void innerProcess(CrawlURI uri) throws InterruptedException {
        byte[] uriBytes = Bytes.toBytes(uri.toString());
        byte[] key = uriBytes;
        Get g = new Get(key);
        try {
            Result r = client.get(g);
            // no data for uri is indicated by empty Result
            if (r.isEmpty()) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine(uri + ": <no crawlinfo>");
                return;
            }
            Map<String, Object>[] history = getFetchHistory(uri);
            Map<String, Object> h0 = history[0] = new HashMap<String, Object>();
            // FetchHTTP ignores history with status <= 0
            byte[] status = r.getValue(COLUMN_FAMILY, COLUMN_STATUS);
            if (status != null) {
                // Note that status is stored as integer text. It's typically three-chars
                // that is less than 4-byte integer bits.
                h0.put(RecrawlAttributeConstants.A_STATUS, Integer.parseInt(Bytes.toString(status)));
                byte[] etag = r.getValue(COLUMN_FAMILY, COLUMN_ETAG);
                if (etag != null) {
                    h0.put(RecrawlAttributeConstants.A_ETAG_HEADER, Bytes.toString(etag));
                }
                byte[] lastmod = r.getValue(COLUMN_FAMILY, COLUMN_LAST_MODIFIED);
                if (lastmod != null) {
                    long lastmod_sec = Bytes.toLong(lastmod);
                    h0.put(RecrawlAttributeConstants.A_LAST_MODIFIED_HEADER, formatHttpDate(lastmod_sec));
                }
                byte[] digest = r.getValue(COLUMN_FAMILY, COLUMN_CONTENT_DIGEST);
                if (digest != null) {
                    h0.put(RecrawlAttributeConstants.A_CONTENT_DIGEST, Bytes.toString(digest));
                }
            }
            if (logger.isLoggable(Level.FINE))
                logger.fine(uri + ": FETCH_HISTORY=" + history);
        } catch (IOException ex) {
            logger.warning("Get failed for " + uri);
        }
    }

    @Override
    protected boolean shouldProcess(CrawlURI uri) {
        // TODO: we want deduplicate robots.txt, too.
        //if (uri.isPrerequisite()) return false;
        String scheme = uri.getUURI().getScheme();
        if (!(scheme.equals("http") || scheme.equals("https"))) return false;
        return true;
    }
}
