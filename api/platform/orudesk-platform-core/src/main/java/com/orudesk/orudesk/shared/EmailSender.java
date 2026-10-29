package com.orudesk.orudesk.shared;

/**
 * Sends an email (Day 52): a port any module can use, with SMTP or the log behind it. Call it from an
 * {@code @ApplicationModuleListener}, after the commit: an email can't be taken back if the transaction then
 * rolls back, and a mail server that's down must not fail the user's request.
 */
public interface EmailSender {

    /** @throws EmailNotSentException if the mail server refused or couldn't be reached: the caller may retry */
    void send(EmailMessage message);
}
