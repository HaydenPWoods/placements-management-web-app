package hpw7.placements;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuring the connection to the (SMTP) mail server, and creating an instance of the {@link JavaMailSender} that can
 * be used to send emails for methods defined in the {@link hpw7.placements.service.MailService}.
 */
@Configuration
@EnableAsync
public class MailSenderConfig {

    /**
     * The host address of the mail server, as defined in application.properties.
     */
    @Value("${spring.mail.host}")
    private String host;

    /**
     * The port that the mail server is running on, as defined in application.properties.
     */
    @Value("${spring.mail.port}")
    private int port;

    /**
     * The username (likely an email) to use for connecting to the mail server and sending messages from, as defined in
     * application.properties.
     */
    @Value("${spring.mail.username}")
    private String emailUsername;

    /**
     * The password to use alongside the username for connecting to the mail server and sending messages, as defined in
     * application.properties.
     */
    @Value("${spring.mail.password}")
    private String emailPassword;

    /**
     * Get a {@link JavaMailSender} instance, configured with the details as provided in application.properties.
     *
     * @return A configured JavaMailSender.
     */
    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

        // Setting SMTP connection details, according to those provided in application.properties
        javaMailSender.setHost(host);
        javaMailSender.setPort(port);
        javaMailSender.setUsername(emailUsername);
        javaMailSender.setPassword(emailPassword);

        // Setting necessary properties
        javaMailSender.getJavaMailProperties().put("mail.smtp.auth", true);
        javaMailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", true);

        return javaMailSender;
    }

}
