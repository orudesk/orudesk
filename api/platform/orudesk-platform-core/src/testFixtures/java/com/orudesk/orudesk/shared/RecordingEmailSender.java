package com.orudesk.orudesk.shared;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * For tests (Day 52): keeps every email instead of sending it, and can fail on purpose. Use it as a @Primary
 * bean in a test configuration, then look at {@link #sentTo(String)}.
 */
public class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private volatile boolean down;

    @Override
    public void send(EmailMessage message) {
        if (down) {
            throw new EmailNotSentException("the test's mail server is down", null);
        }
        sent.add(message);
    }

    public List<EmailMessage> sentTo(String address) {
        return sent.stream().filter(message -> message.to().equalsIgnoreCase(address)).toList();
    }

    /** true: every send fails, as with an unreachable mail server */
    public void setDown(boolean down) {
        this.down = down;
    }
}
