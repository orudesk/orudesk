package com.orudesk.orudesk.identity.adapter.in.event;

import com.orudesk.orudesk.identity.application.port.in.PasswordResetRequested;
import com.orudesk.orudesk.identity.application.port.in.PasswordResetUseCase;
import com.orudesk.orudesk.identity.domain.UserId;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Mails the reset link (Day 53), after the request commits, from the outbox. The token is made here, not in the
 * request: the event is stored as JSON and must not hold a secret. If the email fails, the publication is
 * resubmitted later (Day 54) and a fresh token replaces the one that was never delivered.
 */
@Component
class PasswordResetEmailListener {

    private final PasswordResetUseCase passwordReset;
    private final EmailSender emailSender;
    private final String webBaseUrl;
    private final Duration ttl;

    PasswordResetEmailListener(PasswordResetUseCase passwordReset, EmailSender emailSender,
                               @Value("${orudesk.web.base-url:http://localhost:5173}") String webBaseUrl,
                               @Value("${orudesk.identity.password-reset.ttl:30m}") Duration ttl) {
        this.passwordReset = passwordReset;
        this.emailSender = emailSender;
        this.webBaseUrl = webBaseUrl;
        this.ttl = ttl;
    }

    @ApplicationModuleListener
    void on(PasswordResetRequested event) {
        passwordReset.issueToken(new UserId(event.userId())).ifPresent(token ->
                emailSender.send(new EmailMessage(event.email(), "Reset your Orudesk password", """
                        Hello,

                        Someone asked to reset the password of the Orudesk account %s.

                        Choose a new password here (the link works once, for %d minutes):
                        %s/reset-password?token=%s

                        If it wasn't you, ignore this email: your password stays as it is.
                        """.formatted(event.email(), ttl.toMinutes(), webBaseUrl,
                        URLEncoder.encode(token, StandardCharsets.UTF_8)))));
    }
}
