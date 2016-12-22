package ar.com.grayshirts.commons.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Gets static access to all bean created by the Spring context, the Spring environment
 * and the configuration values.
 */
public class ApplicationContextProvider implements ApplicationContextAware {

	private static ApplicationContext context;

	public static ApplicationContext getApplicationContext() {
		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

	public static Object getBean(String id) {
		return context.getBean(id);
	}

	public static <T> T getBean(Class<T> clazz) throws BeansException {
		return context.getBean(clazz);
	}

	public static Environment getEnvironment() {
		return context.getEnvironment();
	}

	public static String getProperty(String key) {
		return context.getEnvironment().getProperty(key);
	}

	public static String getProperty(String key, String defaultValue) {
		return context.getEnvironment().getProperty(key, defaultValue);
	}
}
