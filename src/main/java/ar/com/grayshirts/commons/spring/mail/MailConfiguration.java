package ar.com.grayshirts.commons.spring.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Properties;


@Configuration
@ConditionalOnClass(javax.mail.Session.class)
public class MailConfiguration {

	private Logger log = LoggerFactory.getLogger(MailConfiguration.class);

	@Value("${spring.mail.username}") private String username;
	@Value("${spring.mail.password}") private String password;
	@Value("${spring.mail.host}") private String host;
	@Value("${spring.mail.port}") private String port;
	@Value("${spring.mail.properties.smtp.auth}") private String smtpAuth;
	@Value("${spring.mail.properties.smtp.starttls.enable}") private String starttlsEnable;

    @Value("${spring.mail.corePoolSize:1}") private int corePoolSize;
    @Value("${spring.mail.maxPoolSize:2}") private int maxPoolSize;
    @Value("${spring.mail.queueCapacity:1000}") private int queueCapacity;

	@Bean
    MailService mailService() {
	    return new MailService();
    }

	@Bean
	JavaMailSender javaMailSender() {
		log.info("Configuring e-mail sender and pool executor for \"{}\" account", username);
		AsyncMailSender asyncMailSender = new AsyncMailSender();
		asyncMailSender.setDefaultEncoding("UTF-8");
		asyncMailSender.setUsername(username);
		asyncMailSender.setPassword(password);
		asyncMailSender.setHost(host);

		Properties props = new Properties();
		props.put("mail.smtp.user", username);
		props.put("mail.smtp.password", password);
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);

		props.put("mail.smtp.auth", smtpAuth);
		props.put("mail.smtp.starttls.enable", starttlsEnable);
		//props.put("mail.smtp.EnableSSL.enable","true");

		asyncMailSender.setJavaMailProperties(props);

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setThreadGroup(asyncMailSender.new LogErrorThreadGroupHandler());
		executor.initialize();

		asyncMailSender.setTaskExecutor(executor);
		return asyncMailSender;
	}
}
