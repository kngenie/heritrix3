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
package org.archive.modules.hq;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpMethod;
import org.archive.modules.CrawlURI;
import org.archive.modules.recrawl.RecrawlAttributeConstants;

/**
 * a bean holding data necessary for sending "finished" event to HQ.
 * <p>As {@link HttpPersistProcessor} submits "finished" event in the background,
 * CrawlURI may be reset, or even already be reused for other URI when submission
 * actually happens. So we need to keep a copy of data in this object.
 * @contributor Kenji Nagahashi
 */
public final class FinishedData {
    private static final Logger logger = Logger.getLogger(HttpHeadquarterAdapter.class.getName());
    
    final String uri;
    final String digest;
    final int status;
    final String etag;
    final long lastModified;
    final String uriid;

    public FinishedData(CrawlURI uri) {
        this.uri = uri.getURI();
        this.digest = uri.getContentDigestSchemeString();
        this.status = uri.getFetchStatus();
        this.etag = getETag(uri);
        this.lastModified = getLastModified(uri);
        Map<String, Object> uridata = uri.getData();
        uriid = uridata != null && uridata.containsKey(HttpHeadquarterAdapter.DATAKEY_ID) ?
            (String)uridata.get(HttpHeadquarterAdapter.DATAKEY_ID) : null;
    }
    // getters compatible with CrawlURI...
    
    public String getURI() {
        return uri;
    }
    public String getContentDigestSchemeString() {
        return digest;
    }
    public int getFetchStatus() {
        return status;
    }
    public String getETag() {
        return etag;
    }
    public long getLastModified() {
        return lastModified;
    }
    
//    protected static final DateFormat HTTP_DATE_FORMAT = 
//        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
//
//    /**
//     * converts time in HTTP Date format {@code dateStr} to seconds
//     * since epoch. 
//     * @param dateStr time in HTTP Date format.
//     * @return seconds since epoch
//     */
//    protected static long parseHttpDate(String dateStr) {
//        synchronized (HTTP_DATE_FORMAT) {
//            try {
//                Date d = HTTP_DATE_FORMAT.parse(dateStr);
//                return d.getTime() / 1000;
//            } catch (ParseException ex) {
//                if (logger.isLoggable(Level.FINER))
//                    logger.fine("bad HTTP DATE: " + dateStr);
//                return 0;
//            }
//        }
//    }
    private static String getHeaderValue(org.apache.commons.httpclient.HttpMethod method, String name) {
        org.apache.commons.httpclient.Header header = method.getResponseHeader(name);
        return header != null ? header.getValue() : null;
    }
    private static String getETag(CrawlURI uri) {
        HttpMethod method = uri.getHttpMethod();
        if (method == null) return null;
        String value = getHeaderValue(method, RecrawlAttributeConstants.A_ETAG_HEADER);
        if (value == null) return null;
        // ETag value is usually quoted - remove quotes.
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
            value = value.substring(1, value.length() - 1);
        return value;
    }
    private static long getLastModified(CrawlURI uri) {
        HttpMethod method = uri.getHttpMethod();
        if (method == null) return 0;
        String lastmod = getHeaderValue(method, RecrawlAttributeConstants.A_LAST_MODIFIED_HEADER);
        long value = 0;
        if (lastmod != null)
            value = HttpHeadquarterAdapter.parseHttpDate(lastmod);
        if (value == 0) {
            // this try-catch is remnant of old code in which data was retrieved from CrawlURI
            // at submission time. it would be no longer necessary.
            try {
                value = uri.getFetchCompletedTime();
            } catch (NullPointerException ex) {
                logger.warning("CrawlURI.getFetchCompletedTime():" + ex + " for " + uri.shortReportLine());
            }
        }
        return value;
    }
}
