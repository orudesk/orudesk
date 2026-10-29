package com.orudesk.orudesk.shared.adapter.out.mail;

import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailNotSentException;
import com.orudesk.orudesk.shared.EmailSender;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Sends through the SMTP server in spring.mail.* (Mailpit locally, the provider's relay in production). */
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.text());
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            throw new EmailNotSentException("could not send \"" + message.subject() + "\"", e);
        }
    }
}
