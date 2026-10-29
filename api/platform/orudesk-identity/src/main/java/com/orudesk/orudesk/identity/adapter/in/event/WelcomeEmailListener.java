package com.orudesk.orudesk.identity.adapter.in.event;

import com.orudesk.orudesk.identity.application.port.in.UserRegistered;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailSender;
import com.orudesk.orudesk.shared.ProcessedEvents;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Welcomes a new user by email (Day 52). An @ApplicationModuleListener: it runs after the registration commits,
 * on another thread, and the event is in the outbox until it succeeds. So a mail server that's down neither
 * fails the registration nor loses the email: it's sent when the publication is retried (Day 54).
 */
@Component
class WelcomeEmailListener {

    private final EmailSender emailSender;
    private final ProcessedEvents processed;
    private final String webBaseUrl;

    WelcomeEmailListener(EmailSender emailSender, ProcessedEvents processed,
                         @Value("${orudesk.web.base-url:http://localhost:5173}") String webBaseUrl) {
        this.emailSender = emailSender;
        this.processed = processed;
        this.webBaseUrl = webBaseUrl;
    }

    @ApplicationModuleListener
    void on(UserRegistered event) {
        // At least once is the outbox's promise; a welcome email twice looks broken. The receipt is written in
        // this listener's transaction: if sending fails, it's rolled back and the retry may send (Day 54).
        if (!processed.firstTime("welcome-email:" + event.userId())) {
            return;
        }
        emailSender.send(new EmailMessage(event.email(), "Welcome to Orudesk", """
                Hello,

                An Orudesk account was created for %s.

                Sign in at %s/login

                If you didn't expect this email, tell your administrator.
                """.formatted(event.email(), webBaseUrl)));
    }
}
