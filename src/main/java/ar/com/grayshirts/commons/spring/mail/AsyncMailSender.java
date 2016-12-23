package ar.com.grayshirts.commons.spring.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import javax.activation.FileTypeMap;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static ar.com.grayshirts.commons.format.StringUtils.maskEmail;


/**
 * Wrapper for {@link org.springframework.mail.javamail.JavaMailSenderImpl JavaMailSenderImpl},
 * that sends emails async, creating a thread on each submit.<br/>
 * the object <i>taskExecutor -> threadGroup</i> manages the exceptions
 * {@link org.springframework.mail.MailException MailException}.
 */
public class AsyncMailSender implements JavaMailSender {

	private static final Logger log = LoggerFactory.getLogger(AsyncMailSender.class);

	private JavaMailSenderImpl mailSender;

	private TaskExecutor taskExecutor;

	public AsyncMailSender() {
		mailSender = new JavaMailSenderImpl();
	}


	@Override public void send(SimpleMailMessage simpleMailMessage) throws MailException {
		taskExecutor.execute(new AsyncMailTask(simpleMailMessage));
	}

	@Override public void send(SimpleMailMessage[] simpleMailMessages)
			throws MailException {
		taskExecutor.execute(new AsyncMailTask(simpleMailMessages));
	}

	@Override public void send(MimeMessage mimeMessage) throws MailException {
		taskExecutor.execute(new AsyncMailTask(mimeMessage));

	}

	@Override public void send(MimeMessage[] mimeMessages) throws MailException {
		taskExecutor.execute(new AsyncMailTask(mimeMessages));
	}

	@Override public void send(MimeMessagePreparator mimeMessagePreparator)
			throws MailException {
		taskExecutor.execute(new AsyncMailTask(mimeMessagePreparator));
	}

	@Override public void send(MimeMessagePreparator[] mimeMessagesPreparator)
			throws MailException {
		taskExecutor.execute(new AsyncMailTask(mimeMessagesPreparator));
	}

	@Override public MimeMessage createMimeMessage() {
		return mailSender.createMimeMessage();
	}

	@Override public MimeMessage createMimeMessage(InputStream inputStream)
			throws MailException {
		return mailSender.createMimeMessage(inputStream);
	}


	private class AsyncMailTask implements Runnable {

		private SimpleMailMessage[] messages = null;
		private MimeMessage[] mimeMessages = null;
		private MimeMessagePreparator[] mimeMessagesPreparator = null;

		private AsyncMailTask(SimpleMailMessage message) {
			this.messages = new SimpleMailMessage[] {message};
		}
		private AsyncMailTask(SimpleMailMessage[] messages) {
			this.messages = messages;
		}

		private AsyncMailTask(MimeMessage message) {
			this.mimeMessages = new MimeMessage[] {message};
		}
		private AsyncMailTask(MimeMessage[] messages) {
			this.mimeMessages = messages;
		}

		private AsyncMailTask(MimeMessagePreparator message) {
			this.mimeMessagesPreparator = new MimeMessagePreparator[] {message};
		}
		private AsyncMailTask(MimeMessagePreparator[] messages) {
			this.mimeMessagesPreparator = messages;
		}

		@Override public void run() {
            if(messages!=null) {
                for(SimpleMailMessage m : messages) {
                    mailSender.send(m);
                    log.debug("E-mail sent to {}",
                              Stream.of(m.getTo()).map(s->maskEmail(s)).collect(Collectors.joining(", ")));
                }
            } else if(mimeMessages!=null) {
                for(MimeMessage m : mimeMessages) {
                    mailSender.send(m);
                    try {
                        log.debug("E-mail sent to {}",
                                  Stream.of(m.getHeader("To")).map(s->maskEmail(s)).collect(Collectors.joining(", ")));
                    } catch (Throwable e) {
                        log.warn("Error debugging e-mail sent.", e);
                    }
                }
            } else {
                for(MimeMessagePreparator m : mimeMessagesPreparator) {
                    mailSender.send(m);
                    log.debug("E-mail sent.");
                }
            }
		}
	}

	/**
	 * Extends from {@link ThreadGroup}, and logs as <b><code>ERROR</code></b>
	 * all the errors.
	 */
	public class LogErrorThreadGroupHandler extends ThreadGroup {

		public LogErrorThreadGroupHandler() {
			super(LogErrorThreadGroupHandler.class.getSimpleName());
		}

		public LogErrorThreadGroupHandler(String name) {
			super(name);
		}

		@Override public void uncaughtException(Thread thread, Throwable throwable) {
			log.error("Error sending e-mail.", throwable);
		}
	}


	/* Getters and Setters */

	public void setMailSender(JavaMailSenderImpl mailSender) {
		this.mailSender = mailSender;
	}
	public void setJavaMailProperties(Properties javaMailProperties) {
		mailSender.setJavaMailProperties(javaMailProperties);
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	public void setSession(Session session) {
		mailSender.setSession(session);
	}
	public void setProtocol(String protocol) {
		mailSender.setProtocol(protocol);
	}
	public void setHost(String host) {
		mailSender.setHost(host);
	}
	public void setPort(int port) {
		mailSender.setPort(port);
	}
	public void setUsername(String username) {
		mailSender.setUsername(username);
	}
	public void setPassword(String password) {
		mailSender.setPassword(password);
	}
	public void setDefaultEncoding(String defaultEncoding) {
		mailSender.setDefaultEncoding(defaultEncoding);
	}
	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		mailSender.setDefaultFileTypeMap(defaultFileTypeMap);
	}
	public JavaMailSenderImpl getMailSender() {
		return mailSender;
	}
	public Properties getJavaMailProperties() {
		return mailSender.getJavaMailProperties();
	}
	public Session getSession() {
		return mailSender.getSession();
	}
	public String getProtocol() {
		return mailSender.getProtocol();
	}
	public String getHost() {
		return mailSender.getHost();
	}
	public int getPort() {
		return mailSender.getPort();
	}
	public String getUsername() {
		return mailSender.getUsername();
	}
	public String getPassword() {
		return mailSender.getPassword();
	}
	public String getDefaultEncoding() {
		return mailSender.getDefaultEncoding();
	}
	public FileTypeMap getDefaultFileTypeMap() {
		return mailSender.getDefaultFileTypeMap();
	}
}
