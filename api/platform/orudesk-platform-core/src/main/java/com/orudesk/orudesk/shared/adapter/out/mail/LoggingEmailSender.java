package com.orudesk.orudesk.shared.adapter.out.mail;

import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No mail server configured: the email goes to the log instead. Only the subject and recipient at INFO; the
 * body (which may hold a password reset link) only at DEBUG, so it isn't in production logs by accident.
 */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    LoggingEmailSender() {
        log.warn("No mail server configured (spring.mail.host): emails are written to the log, not sent");
    }

    @Override
    public void send(EmailMessage message) {
        log.info("Email not sent (no mail server): \"{}\" to {}", message.subject(), message.to());
        log.debug("Email body:\n{}", message.text());
    }
}
