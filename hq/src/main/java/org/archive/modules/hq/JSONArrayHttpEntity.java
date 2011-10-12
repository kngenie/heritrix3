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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @contributor kenji
 */
public class JSONArrayHttpEntity implements HttpEntity {
    protected JSONArray jsonArray;
    protected boolean compress;
    private byte[] data;

    public JSONArrayHttpEntity(JSONArray jsonArray, boolean compress) {
        this.jsonArray = jsonArray;
        this.compress = compress;
    }
    public JSONArrayHttpEntity(JSONArray jsonArray) {
        this(jsonArray, false);
    }
    
    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#consumeContent()
     */
    @Override
    public void consumeContent() throws IOException {
    }

    protected byte[] getBytes() throws IOException {
        if (data == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = compress ? new GZIPOutputStream(baos) : baos;
            Writer w = new OutputStreamWriter(os, "UTF-8");
            try {
                jsonArray.write(w);
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            w.close();
            if (compress) os.close();
            data = baos.toByteArray();
        }
        return data;
    }
    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#getContent()
     */
    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        return new ByteArrayInputStream(getBytes());
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#getContentEncoding()
     */
    @Override
    public Header getContentEncoding() {
        if (compress) {
            return new BasicHeader("Content-Encoding", "gzip");
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#getContentLength()
     */
    @Override
    public long getContentLength() {
        try {
            return getBytes().length;
        } catch (IOException ex) {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#getContentType()
     */
    @Override
    public Header getContentType() {
        return new BasicHeader("Content-Type", "text/json");
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#isChunked()
     */
    @Override
    public boolean isChunked() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#isRepeatable()
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#isStreaming()
     */
    @Override
    public boolean isStreaming() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.http.HttpEntity#writeTo(java.io.OutputStream)
     */
    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        outstream.write(getBytes());
    }

}
