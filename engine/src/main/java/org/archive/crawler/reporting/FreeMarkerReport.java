package org.archive.crawler.reporting;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.archive.crawler.framework.CrawlController;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;

import freemarker.ext.beans.BeanModel;
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
 * value will be replaced by its {@link Reporter#shortReportMap()}. It also has utility
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
			// to keys of the map. It is very confusing. Alternatively we
			setSimpleMapWrapper(true);
		}

		@Override
		public TemplateModel wrap(Object object) throws TemplateModelException {
			if (object instanceof Reporter) {
				return new ReportModel((Reporter)object, this);
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
		public RootReportModel(CrawlController controller) {
			super(controller, theWrapperInstance);
		}
		@Override
		public TemplateModel get(String key) throws TemplateModelException {
			// Use default BreansWrapper for these members for exposing methods.
			// (assuming default BeansWrapper instance's exposure level is unchanged).
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

	private String templateName;

	public FreeMarkerReport(String templateName) {
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
			template.process(new RootReportModel(controller), writer);
			writer.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (TemplateException ex) {
			throw new RuntimeException(ex);
		}
	}

}