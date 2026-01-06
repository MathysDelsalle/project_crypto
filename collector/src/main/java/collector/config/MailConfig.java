package collector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${SPRING_MAIL_HOST:${MAIL_HOST}}") String host,
            @Value("${SPRING_MAIL_PORT:${MAIL_PORT:587}}") int port,
            @Value("${SPRING_MAIL_USERNAME:${MAIL_USERNAME}}") String username,
            @Value("${SPRING_MAIL_PASSWORD:${MAIL_PASSWORD}}") String password
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // ✅ FORCER STARTTLS
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        // (optionnel) pour debug voir le handshake STARTTLS
        props.put("mail.debug", "true");

        return sender;
    }
}
