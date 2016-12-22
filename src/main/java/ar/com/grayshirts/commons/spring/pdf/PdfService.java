package ar.com.grayshirts.commons.spring.pdf;

import ar.com.grayshirts.commons.spring.template.TemplateService;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.stream.Stream;


/**
 * Service class to generate PDF reports from an HTML/Velocity template.
 */
public class PdfService {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${pdf.enable:true}")
	private boolean enable;

    @Value("${pdf.author:Grayshirts}")
    private String author;

    private Document document = new Document(PageSize.A4, 50, 50, 70, 70);

	@Autowired
	private Environment environment;

    @Autowired
    private TemplateService templateService;

	/**
	 * Creates a PDF file using `templatePath` velocity template and `context`environment variables.
     *
     * Uses by default A4 size, you can change the configuration page with {@link #setDocument(Document)}.
	 *
	 * @param templatePath the Velocity template path. Relative to `src/main/resources/templates/pdf/` path.
     * @param layout the base layout template.  Relative to `src/main/resources/templates/layouts/pdf/` path.
	 * @param title the meta-title of the PDF
	 * @param out the output stream where to place the PDF content. IMPORTANT: you have to close this stream after
	 *            use its content
	 * @param context map with all values to inject to the template
	 */
	public void render(String templatePath, String layout, String title, FileOutputStream out, Map<String, Object> context) {

		// Create the HTML content
        String bodyHtml = templateService.render("../pdf/" + templatePath, "pdf/" + layout, context);

		if (enable) {
			// Generate the PDF file with the generated HTML content
			if (Stream.of(environment.getActiveProfiles()).noneMatch(s -> "prod".equals(s))) {
				for (String profile : environment.getActiveProfiles()) {
					title = "[" + profile.toUpperCase() + "] " + title;
				}
			}
			log.debug(getLog(title, bodyHtml));
			PdfWriter pdfWriter;
			try {
				pdfWriter = PdfWriter.getInstance(document, out);
			} catch (Exception e) {
				throw new RuntimeException("Error creating the PDF report.", e);
			}
			pdfWriter.createXmpMetadata();
			document.addTitle(title);
			document.addAuthor(author);
			document.addCreationDate();
			document.open();
			XMLWorkerHelper worker = XMLWorkerHelper.getInstance();

			try {
				worker.parseXHtml(pdfWriter, document, new StringReader(bodyHtml));
			} catch (IOException e) {
				throw new RuntimeException("Error trying to generate a PDF report.", e);
			} finally {
				document.close();
			}
		} else {
			log.info(getLog(title, bodyHtml));
		}
	}

	private String getLog(String title, String text) {
		return "Creating PDF report with title \"" + title + "\" and body: " + text;
	}

    public Document getDocument() {
        return document;
    }
    public void setDocument(Document document) {
        this.document = document;
    }
}
