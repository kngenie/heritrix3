package org.archive.crawler.reporting;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.archive.crawler.framework.CrawlController;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;
import org.archive.util.TemplateReporter;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
/**
 * FreeMarkerReport generates report with FreeMarker template.
 * 
 * extend this class, pass template name (including {@code .ftl} suffix if it has one,
 * but no package part, which is assumed to be {@code org.archive.crawler.reporting})
 * to super constructor. template will receive {@link ReportModel} object as root
 * variable. It gives read-only access to all {@link CrawlController} properties.
 * For those properties returning objects implementing {@link Reporter} interface,
 * value will be replaced by its {@link Reporter#reportMap()}. It also has utility
 * methods useful in templates. Static methods in {@link ArchiveUtils} are available
 * through {@code archiveUtils} variable. 
 * 
 * As {@link ReportModel} uses reflection for accessing getters, it shouldn't be necessary
 * to modify it when properties are added to/removed from {@link CrawlController}.
 * 
 * @author Kenji Nagahashi
 *
 */
public abstract class FreeMarkerReport extends Report {

	public static class ReportModel {
		private Object target;
		private Map<String, Object> reportMap = null;
		public ReportModel(Object target) {
			this.target = target;
			if (this.target instanceof TemplateReporter) {
				this.reportMap = ((TemplateReporter)this.target).reportMap();
			} else if (this.target instanceof CrawlController) {
				this.reportMap = new HashMap<String, Object>();
			}
		}
		
		/**
		 * return JavaBeans property {@code key} of {@link CrawlController}. if
		 * property value is an Object implementing {@link Reporter} interface,
		 * invokes {@link Reporter#reportMap()} method and returns its
		 * return value instead.
		 * @param key property name
		 * @return simple property value, or a Map
		 * @throws IllegalArgumentException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public Object get(String key) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
			if (reportMap != null && reportMap.containsKey(key))
				return reportMap.get(key);
			Method m = findGetter(key);
			if (m == null) return null;
			Object obj = m.invoke(target, new Object[0]);
			if (obj instanceof TemplateReporter) {
				obj = new ReportModel(obj);
				reportMap.put(key,  obj);
			}
			return obj;
		}
		protected Method findGetter(String property) {
			String capped = Character.toUpperCase(property.charAt(0)) + property.substring(1);
			try {
				return target.getClass().getMethod("get" + capped, new Class[0]);
			} catch (NoSuchMethodException e1) {
				try {
				return target.getClass().getMethod("is" + capped, new Class[0]);
				} catch (NoSuchMethodException e2) {
					return null;
				}
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

	private String templateName;

	public FreeMarkerReport(String templateName) {
		super();
		this.templateName = templateName;
	}

	public String getTemplateName() {
		return templateName;
	}
	
	@Override
	public void write(PrintWriter writer, StatisticsTracker stats) {
		write(writer, stats.controller);
	}
	
	public void write(PrintWriter writer, CrawlController controller) {
		Configuration tmplConfig = new Configuration();
		tmplConfig.setClassForTemplateLoading(getClass(), "");
		
		// TODO: think about better error reporting interface than throwing RuntimeException.
		try {
			Template template = tmplConfig.getTemplate(templateName);
			template.process(new ReportModel(controller), writer);
			writer.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (TemplateException ex) {
			throw new RuntimeException(ex);
		}
	}

}