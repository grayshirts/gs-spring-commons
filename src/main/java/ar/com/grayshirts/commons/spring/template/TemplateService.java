package ar.com.grayshirts.commons.spring.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.StringWriter;
import java.util.Map;
import static org.apache.commons.lang3.StringUtils.isEmpty;


/**
 * Service to render strings from Velocity template engine.
 */
public class TemplateService {

    private Logger log = LoggerFactory.getLogger(TemplateService.class);

    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private String velocityTemplateBasePath;

    /**
     * Render the template `templatePath` into a String, using the values contained in `context` as variables.
     *
     * @param templatePath the Velocity template path. Relative to `src/main/resources/templates/` path.
     * @param layout the base layout template.  Relative to `src/main/resources/templates/layouts` path.
     * @param context map with all values to inject to the template
     */
    public String render(String templatePath, String layout, Map<String, Object> context) {
        try {
            if (isEmpty(templatePath)) throw new NullPointerException("\"templatePath\" cannot be null or empty.");
            if (isEmpty(layout)) throw new NullPointerException("\"layout\" cannot be null or empty.");

            log.debug("Rendering template \"{}\" ...", templatePath);
            Template template = velocityEngine.getTemplate(velocityTemplateBasePath + "layouts/" + layout + ".vm", "UTF-8");
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("body", "../" + templatePath + ".vm");
            if (context != null) context.forEach((k, v) -> velocityContext.put(k, v));
            StringWriter writer = new StringWriter();
            template.merge(velocityContext, writer);
            String text = writer.toString();
            log.debug("Rendering template \"{}\" done. Output: {}", templatePath, text);
            return text;
        } catch (Throwable e) {
            log.error("Error rendering template \"" + templatePath + "\".", e);
            throw e;
        }
    }
}
