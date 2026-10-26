package com.orudesk.orudesk.audit.adapter.in.event;

import com.orudesk.orudesk.audit.application.port.in.Actor;
import com.orudesk.orudesk.audit.application.port.in.RecordAuditEntryUseCase;
import com.orudesk.orudesk.shared.AuditableEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hears every AuditableEvent, from any module, and records it with who did it and in which request.
 *
 * A plain @EventListener runs at once, on the publisher's thread, inside its transaction: the entry commits or
 * rolls back with the change, and the logged-in user and request id are still at hand. (Spring Modulith's
 * @ApplicationModuleListener would run after the commit, asynchronously: right for sending an email, wrong for
 * an audit log, where "changed but not recorded" must not happen.)
 */
@Component
class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final RecordAuditEntryUseCase recordAuditEntry;

    AuditEventListener(RecordAuditEntryUseCase recordAuditEntry) {
        this.recordAuditEntry = recordAuditEntry;
    }

    @EventListener
    void on(AuditableEvent event) {
        Actor actor = Actor.NOBODY;
        UUID tenantId = event.tenantId();
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            actor = new Actor(UUID.fromString(token.getToken().getSubject()), token.getToken().getClaimAsString("email"));
            tenantId = UUID.fromString(token.getToken().getClaimAsString("tenant_id"));
        }
        if (tenantId == null) {
            // Nobody logged in and the event doesn't say where: there's no tenant to file it under
            log.warn("Not audited, no tenant known: {} {}", event.action(), event.subjectId());
            return;
        }
        recordAuditEntry.record(event, tenantId, actor, MDC.get("request_id"));
    }
}
