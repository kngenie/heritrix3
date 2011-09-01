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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.context.Lifecycle;

/**
 * @contributor kenji
 */
public class HBaseClient implements Lifecycle {
    private HTable table;
    
    protected String zookeeperQuorum = "localhost";
    public void setZookeeperQuorum(String zookeeperQuorum) {
        this.zookeeperQuorum = zookeeperQuorum;
    }
    public String getZookeeperQuorum() {
        return zookeeperQuorum;
    }
    protected String htableName = "crawlinfo";
    public void setHtableName(String htableName) {
        this.htableName = htableName;
    }
    public String getHtableName() {
        return htableName;
    }
    
    public HBaseClient() {
    }
    
    public void put(Put p) throws IOException {
        // HTable.put() buffers Puts and access to the buffer is not
        // synchronized.
        synchronized (table) {
            table.put(p);
        }
    }
    
    public Result get(Get g) throws IOException {
        return table.get(g);
    }
    
    @Override
    public boolean isRunning() {
        return table != null;
    }
    @Override
    public void start() {
        // TODO if multiple HTable are used, they should share Configuration
        // object. we may want to cut out HBaseConfiguration as separate bean,
        // or make single HBaseClient capable of handling multiple HTables. 
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
        try {
            table = new HTable(conf, Bytes.toBytes(htableName));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    @Override
    public void stop() {
        table = null;
    }
}
