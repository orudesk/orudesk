package com.orudesk.orudesk.shared;

import java.util.Objects;

/**
 * One plain-text email (Day 52). Plain text on purpose: it reads the same in every mail client, can't hide a
 * link behind different text, and needs no template engine. HTML can come when a design needs it.
 *
 * @param to      one address; an email per recipient, so nobody sees anyone else's address
 * @param subject one line
 * @param text    the body
 */
public record EmailMessage(String to, String subject, String text) {

    public EmailMessage {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(text, "text must not be null");
        if (subject.contains("\n") || subject.contains("\r")) {
            throw new IllegalArgumentException("a subject is one line");   // no header injection through a name
        }
    }
}
