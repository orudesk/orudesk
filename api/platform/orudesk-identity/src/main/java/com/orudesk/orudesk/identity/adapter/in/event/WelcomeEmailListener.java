package com.orudesk.orudesk.identity.adapter.in.event;

import com.orudesk.orudesk.identity.application.port.in.UserRegistered;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Welcomes a new user by email (Day 52). An @ApplicationModuleListener: it runs after the registration commits,
 * on another thread, and the event is in the outbox until it succeeds. So a mail server that's down neither
 * fails the registration nor loses the email: it's sent when the publication is resubmitted (Day 54).
 */
@Component
class WelcomeEmailListener {

    private final EmailSender emailSender;
    private final String webBaseUrl;

    WelcomeEmailListener(EmailSender emailSender,
                         @Value("${orudesk.web.base-url:http://localhost:5173}") String webBaseUrl) {
        this.emailSender = emailSender;
        this.webBaseUrl = webBaseUrl;
    }

    @ApplicationModuleListener
    void on(UserRegistered event) {
        emailSender.send(new EmailMessage(event.email(), "Welcome to Orudesk", """
                Hello,

                An Orudesk account was created for %s.

                Sign in at %s/login

                If you didn't expect this email, tell your administrator.
                """.formatted(event.email(), webBaseUrl)));
    }
}
