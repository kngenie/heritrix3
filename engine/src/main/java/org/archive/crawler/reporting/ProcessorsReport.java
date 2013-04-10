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
package org.archive.crawler.reporting;


/**
 * The "Processors Report", delegated through the CrawlController 
 * to each Processor to dump whatever information it collects for 
 * this purpose.
 * 
 * @contributor gojomo
 */
public class ProcessorsReport extends FreeMarkerReport {
	public ProcessorsReport() {
		super("Processor.ftl");
	}

    @Override
    public String getFilename() {
        return "processors-report.txt";
    }

}
