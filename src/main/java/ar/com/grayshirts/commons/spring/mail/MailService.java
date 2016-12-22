package ar.com.grayshirts.commons.spring.mail;

import ar.com.grayshirts.commons.spring.template.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * Service class to send emails from an HTML/Velocity template.
 */
public class MailService {

	private Logger log = LoggerFactory.getLogger(MailService.class);

	@Value("${spring.mail.username}")
	private String username;

    @Value("${spring.mail.senderDomain}")
    private String senderDomain;

    @Value("${spring.mail.senderName}")
    private String senderName;

	@Value("${spring.mail.enable}")
	private boolean enable;

	@Value("${spring.mail.cco}")
	private String cco;

	@Autowired
	private Environment environment;

	@Autowired
	private JavaMailSender javaMailSender;

    @Autowired
    private TemplateService templateService;

	/**
	 * Send an e-mail in async way.
	 *
	 * @param templatePath the Velocity template path. Relative to `src/main/resources/templates/emails/` path.
     * @param layout the base layout template.  Relative to `src/main/resources/templates/emails/layouts/` path.
	 * @param subject the title of the email
	 * @param to the "To" address
	 * @param context map with all values to inject to the template
	 */
	public void send(String templatePath, String layout, String subject, String to,
                     Map<String, Object> context) {
		send(templatePath, layout, subject, new String[] { to }, context, null);
	}

    /**
     * Send an e-mail in async way.
     *
     * @param templatePath the Velocity template path. Relative to `src/main/resources/templates/emails/` path.
     * @param layout the base layout template.  Relative to `src/main/resources/templates/emails/layouts/` path.
     * @param subject the title of the email
     * @param to the "To" address
     * @param context map with all values to inject to the template
     * @param attachments map with attachment files, the key is the filename, and the value must be
     *                    a {@link File} or an {@link InputStreamSource}
     */
    public void send(String templatePath, String layout, String subject, String to,
                     Map<String, Object> context, Map<String, Object> attachments) {
        send(templatePath, layout, subject, new String[] { to }, context, attachments);
    }

	/**
	 * Send an e-mail in async way.
	 *
	 * @param templatePath the Velocity template path. Relative to `src/main/resources/templates/emails/` path.
     * @param layout the base layout template.  Relative to `src/main/resources/templates/layouts/emails/` path.
	 * @param subject the title of the email
	 * @param to an array with the "To" addresses
	 * @param context map with all values to inject to the template
     * @param attachments map with attachment files, the key is the filename, and the value must be
     *                    a {@link File} or an {@link InputStreamSource}
	 */
	public void send(String templatePath, String layout, String subject, String[] to,
                     Map<String, Object> context, Map<String, Object> attachments) {
		if (to==null || to.length==0) {
			throw new NullPointerException("\"to\" cannot be null or empty.");
		}

		// Create the HTML content
		String text = templateService.render("../emails/" + templatePath, "emails/" + layout, context);

        if (enable) {
			// Send the email
            boolean hasAttachments = attachments!=null && attachments.size()>0;
			try {
				if (Stream.of(environment.getActiveProfiles()).noneMatch(s -> "prod".equals(s))) {
					for (String profile : environment.getActiveProfiles()) {
						subject = "[" + profile.toUpperCase() + "] " + subject;
					}
				}
				log.debug(getLog(subject, text, to));
				MimeMessage mimeMessage = javaMailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachments, "utf-8");
				helper.setTo(to);
				if (isNotEmpty(cco)) {
					helper.setBcc(cco.split(","));
				}

                // Username cannot be null
                if(isEmpty(senderDomain))
                    senderDomain = username;
                if(isEmpty(senderName)) {
                    senderName = username;
                }
				helper.setReplyTo(new InternetAddress(senderDomain, senderName));
				helper.setFrom(new InternetAddress(senderDomain, senderName));
				helper.setSubject(subject);
				if (hasAttachments) {
				    for (String attchName : attachments.keySet()) {
				        Object obj = attachments.get(attchName);
                        if (obj instanceof File) {
                            helper.addAttachment(attchName, new FileDataSource((File) obj));
                        } else if (obj instanceof InputStreamSource) {
                            helper.addAttachment(attchName, (InputStreamSource) obj);
                        } else {
                            throw new IllegalArgumentException(
                                "Illegal object class \"" + obj.getClass().getSimpleName() + "\" for attachment \"" + attchName + "\". " +
                                    "Only object of class \"java.io.File\" or \"org.springframework.core.io.InputStreamSource\" allowed.");
                        }
                    }
                    helper.setText(text, true);
                } else {
                    mimeMessage.setContent(text, "text/html; charset=utf-8");
                }
				javaMailSender.send(mimeMessage);
			} catch (MessagingException e) {
				throw new RuntimeException("Error creating mail message.", e);
			}
            catch(UnsupportedEncodingException e) {
                throw new RuntimeException("Error with sender domain address.", e);
            }
		} else {
			log.info(getLog(subject, text, to));
		}
	}

	private String getLog(String subject, String text, String ... to) {
		return "Sending email to [" + Stream.of(to).collect(Collectors.joining(", ")) + "] with subject \"" + subject + "\" and body: " + text;
	}
}
