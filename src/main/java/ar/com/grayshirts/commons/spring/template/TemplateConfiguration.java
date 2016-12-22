package ar.com.grayshirts.commons.spring.template;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.util.stream.Stream;

@Configuration
@ConditionalOnClass(VelocityEngine.class)
public class TemplateConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public TemplateService templateService() {
        return new TemplateService();
    }

    @Bean
    public VelocityEngine velocityEngine() {
        VelocityEngine velocity = new VelocityEngine();
        if (Stream.of(environment.getActiveProfiles()).noneMatch(s -> "dev".equals(s) || "sandbox".equals(s))) {
            // Get the template from the compiled resource folder (in no dev environments the source code isn't available)
            velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        }
        velocity.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());
        velocity.init();
        return velocity;
    }

    @Bean
    public String velocityTemplateBasePath() {
        String templateBasePath = "templates/";
        if (Stream.of(environment.getActiveProfiles()).anyMatch(s -> "dev".equals(s) || "sandbox".equals(s))) {
            // Get the template from the source code folder to get the latest content without restart
            templateBasePath = "src/main/resources/" + templateBasePath;
        }
        return templateBasePath;
    }
}
