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
package org.archive.util;

import java.util.Map;

/**
 * {@code TemplateReporter} signifies that a object supports template-based report
 * generation.
 * <p>This is similar to {@link Reporter} interface, but {@code TemplateReporter}
 * just exposes its properties through {@link #reportMap()} method and does not do
 * any formatting by itself.</p>
 * <p>Hopefully this interface should be replaced by better, standard-based interface
 * like those provided in {@code javax.management}.</p>
 * @see Reporter
 */
public interface TemplateReporter {

	/**
	 * Generates set of key-value pairs for full reporting by templates.
	 * @return a Map, typically a superset of what {@link Reporter#shortReportMap()}
	 * returns.
	 */
	public Map<String, Object> reportMap();
}
