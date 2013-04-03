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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.archive.crawler.framework.CrawlController;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Traditional report of all ToeThread call-stacks, as often consulted
 * to diagnose live crawl issues. 
 * 
 * @contributor gojomo
 */
public class ToeThreadsReport extends Report {
	
	public static class ReportModel {
		private CrawlController controller;
		public ReportModel(CrawlController controller) {
			this.controller = controller;
		}
		
		/**
		 * return JavaBeans property {@code key} of {@link CrawlController}. if
		 * property value is an Object implementing {@link Reporter} interface,
		 * invokes {@link Reporter#shortReportMap()} method and returns its
		 * return value instead.
		 * @param key property name
		 * @return simple property value, or a Map
		 * @throws IllegalArgumentException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public Object get(String key) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
			try {
				Method m = controller.getClass().getMethod(
						"get" + Character.toUpperCase(key.charAt(0)) + key.substring(1), new Class[0]);
				Object obj = m.invoke(controller, new Object[0]);
				if (obj instanceof Reporter) {
					return ((Reporter)obj).shortReportMap();
				} else {
					return obj;
				}
			} catch (NoSuchMethodException ex) {
				return null;
			}
		}
		public long currentTimeMillis() {
			return System.currentTimeMillis();
		}
		public TemplateModel getArchiveUtils() throws TemplateModelException {
			BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
			TemplateHashModel staticModels = wrapper.getStaticModels();
			return staticModels.get(ArchiveUtils.class.getName());
		}
	}

    @Override
    public void write(PrintWriter writer, StatisticsTracker stats) {
        //writer.print(stats.controller.getToeThreadReport());
    	Configuration tmplConfig = new Configuration();
    	tmplConfig.setClassForTemplateLoading(getClass(), "");
    	
    	// TODO: think about better error reporting interface than throwing RuntimeException.
    	try {
    		Template template = tmplConfig.getTemplate("ToeThreads.ftl");
    		template.process(new ReportModel(stats.controller), writer);
    		writer.flush();
    	} catch (IOException ex) {
    		throw new RuntimeException(ex);
    	} catch (TemplateException ex) {
    		throw new RuntimeException(ex);
    	}
    }

    @Override
    public String getFilename() {
        return "threads-report.txt";
    }
    
}
