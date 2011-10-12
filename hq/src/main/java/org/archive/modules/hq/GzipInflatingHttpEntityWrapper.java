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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * {@link HttpEntityWrapper} for inflating (decompressing) response body.
 * Adapted from: http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientGZipContentCompression.java
 * @contributor kenji
 */
public class GzipInflatingHttpEntityWrapper extends HttpEntityWrapper {
    public GzipInflatingHttpEntityWrapper(final HttpEntity entity) {
        super(entity);
    }
    @Override
    public InputStream getContent() throws IOException {
        // this may throw IllegalStateException
        InputStream is = wrappedEntity.getContent();
        return new GZIPInputStream(is);
    }
}
