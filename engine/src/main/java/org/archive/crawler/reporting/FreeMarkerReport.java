package org.archive.crawler.reporting;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.archive.crawler.framework.CrawlController;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.InvalidPropertyException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
/**
 * FreeMarkerReport generates report with FreeMarker template.
 * <p>
 * extend this class, pass template name (including {@code .ftl} suffix if it has one,
 * but no package part, which is assumed to be {@code org.archive.crawler.reporting})
 * to super constructor. 
 * </p>
 * <p>
 * template will receive an instance of {@link RootReportModel}, an extension of
 * {@link BeanModel} that wraps {@link CrawlController}, as root context.
 * This gives templates read-only access to all of {@link CrawlController} properties
 * and methods.
 * </p>
 * <p>
 * RoortReportModel also defines following template variables for accessing additional
 * information and template support services:
 * <ul>
 * <li>{@code stats}: {@link StatisticsTracker} object.</li>
 * <li>{@code currentTimeMillis}: {@link System#currentTimeMillis()} method.</li>
 * <li>{@code archiveUtils}: access to static methods of {@link ArchiveUtils}.</li>
 * </ul>
 * <p>
 * As {@link RootReportModel} uses reflection for accessing getters, it shouldn't be necessary
 * to modify it when properties are added to/removed from {@link CrawlController}.
 * </p>
 * @author Kenji Nagahashi
 *
 */
public class FreeMarkerReport extends Report {

	/**
	 * FreeMarker BeansWrapper that wraps {@link Reporter} objects with {@link ReportModel}.
	 */
	public static class ReportBeansWrapper extends BeansWrapper {
		public ReportBeansWrapper() {
			/*
			 *  It must be a good idea not to let templates access methods,
			 *  but some components (ex. UriUniqFilter, Processor) neither
			 *  implement Reporter interface, nor expose information in JavaBeans way.
			 *  Disabled exposure restriction for now.
			 */
			//setExposureLevel(EXPOSE_PROPERTIES_ONLY);

			// use SimpleMapModel, or ?key builtin returns all Map methods in addition
			// to keys of the map. It is very confusing.
			setSimpleMapWrapper(true);
		}

		@Override
		public TemplateModel wrap(Object object) throws TemplateModelException {
			// if object is a Reporter, use existing Reporter#shortReportMap as a
			// property set. BeanModel could handle this automatically if Reporter
			// had get(String) method.
			if (object instanceof Reporter) {
				return new ReportModel((Reporter)object, this);
			}
			// special handling for CrawStatSnapshot, which only has public fields
			// and non-JavaBeans-compliant getters. TODO: it is best to make CrawlStatSnapshot
			// JavaBeans compliant.
			if (object instanceof CrawlStatSnapshot) {
				return new DirectFieldModel(object, this);
			}
			return super.wrap(object);
		}
	}

	private static final ReportBeansWrapper theWrapperInstance = new ReportBeansWrapper();

	/**
	 * FreeMarker {@link TemplateModel} for use as root-level map passed to
	 * {@link Template#process(Object, java.io.Writer)}.
	 * It wraps {@link CrawlController} to provide reflective access to its properties,
	 * and also exports a few helper methods.
	 */
	public static class RootReportModel extends BeanModel {
		// BeansWrapper for exposing methods to templates. Assuming the default instance's
		// exposure level is unchanged from the default value EXPOSE_SAFE.
		private static final BeansWrapper methodsBeansWrapper = BeansWrapper.getDefaultInstance();
		private StatisticsTracker stats;
		public RootReportModel(StatisticsTracker stats) {
			super(stats.controller, theWrapperInstance);
			this.stats = stats;
		}
		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			// Use default BreansWrapper for these members for exposing methods.
			// (assuming default BeansWrapper instance's exposure level is unchanged).
			if (key.equals("stats")) {
				return wrapper.wrap(stats);
			}
			if (key.equals("currentTimeMillis")) {
				TemplateModel system = methodsBeansWrapper.getStaticModels().get(System.class.getName());
				return ((TemplateHashModel)system).get(key);
			}
			if (key.equals("archiveUtils")) {
				return methodsBeansWrapper.getStaticModels().get(ArchiveUtils.class.getName());
			}
			return super.get(key);
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
	}

	/**
	 * FreeMarker BeanModel extended for pulling data out of {@link Reporter#shortReportMap()}
	 * in preference to reflective access.
	 */
	public static class ReportModel extends BeanModel {
		private Map<String, Object> reportMap;
		public ReportModel(Reporter object, BeansWrapper wrapper) {
			super(object, wrapper);
			this.reportMap = object.shortReportMap();
		}
		public TemplateModel get(String key) throws TemplateModelException {
			if (reportMap.containsKey(key)) {
				return wrap(reportMap.get(key));
			}
			return super.get(key);
		}
	}

	/**
	 * TemplateModle that exposes public fields and public no-arg methods as
	 * properties. Used to wrap non-JavaBeans-compliant object like
	 * {@link CrawlStatSnapshot}. This model does not support JavaBeans properties
	 * or methods with arguments.
	 * <p>
	 * Consider this as transitional measure. It is better to make target object
	 * JavaBeans-compliant.
	 * </p>
	 */
	public static class DirectFieldModel implements TemplateHashModel {
		private Object object;
		private BeansWrapper wrapper;
		public DirectFieldModel(Object object, BeansWrapper wrapper) {
			this.object = object;
			this.wrapper = wrapper;
		}
		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			Object value = wrapMethodOrField(key);
			return wrapper.wrap(value);
		}

		protected Object wrapMethodOrField(String key)
				throws TemplateModelException {
			try {
				Method method = object.getClass().getMethod(key);
				return method.invoke(object);
			} catch (NoSuchMethodException ex) {
			} catch (IllegalAccessException ex) {
			} catch (InvocationTargetException ex) {
				throw new TemplateModelException(ex);
			}
			try {
				Field field = object.getClass().getField(key);
				return field.get(object);
			} catch (NoSuchFieldException ex) {
				throw new InvalidPropertyException(
						"no such method or property: " + key);
			} catch (IllegalAccessException ex) {
				throw new InvalidPropertyException("cannot access field: " +
						key);
			}
		}

		@Override
		public boolean isEmpty() {
			return false;
		}
		
	}

	private String templateName;
	private String filename;

	public FreeMarkerReport(String templateName, String filename) {
		this.templateName = templateName;
		this.filename = filename;
	}

	public String getTemplateName() {
		return templateName;
	}
	
	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void write(PrintWriter writer, StatisticsTracker stats) {
		Configuration tmplConfig = new Configuration();
		tmplConfig.setClassForTemplateLoading(getClass(), "");

		// TODO: think about better error reporting interface than throwing RuntimeException.
		try {
			Template template = tmplConfig.getTemplate(templateName);
			template.process(new RootReportModel(stats), writer);
			writer.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (TemplateException ex) {
			throw new RuntimeException(ex);
		}
	}

}