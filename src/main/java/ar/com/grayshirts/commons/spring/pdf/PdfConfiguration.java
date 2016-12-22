package ar.com.grayshirts.commons.spring.pdf;

import ar.com.grayshirts.commons.spring.template.TemplateService;
import com.itextpdf.text.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Document.class)
@ConditionalOnBean(TemplateService.class)
public class PdfConfiguration {

    @Bean
    public PdfService pdfService() {
        return new PdfService();
    }
}
