package com.orudesk.orudesk.shared.adapter.out.mail;

import com.orudesk.orudesk.shared.EmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Which EmailSender an app gets: SMTP when spring.mail.host is set (Spring Boot then creates a JavaMailSender),
 * otherwise one that writes each email to the log. A fresh checkout runs without a mail server, and a
 * self-hoster who hasn't configured one sees in the log what would have been sent.
 */
@Configuration
class MailConfiguration {

    @Bean
    EmailSender emailSender(ObjectProvider<JavaMailSender> javaMailSender,
                            @Value("${orudesk.mail.from:Orudesk <no-reply@orudesk.local>}") String from) {
        JavaMailSender smtp = javaMailSender.getIfAvailable();
        return smtp == null ? new LoggingEmailSender() : new SmtpEmailSender(smtp, from);
    }
}
