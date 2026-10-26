package com.orudesk.orudesk.audit.adapter.in.web;

import com.orudesk.orudesk.audit.application.port.in.AuditPage;
import com.orudesk.orudesk.audit.application.port.in.AuditSearch;
import com.orudesk.orudesk.audit.application.port.in.AuditTrailQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-entries")
@Tag(name = "Audit")
class AuditController {

    private final AuditTrailQuery auditTrail;

    AuditController(AuditTrailQuery auditTrail) {
        this.auditTrail = auditTrail;
    }

    /** GET /api/v1/audit-entries?subjectId=...&size=50&after=... : what happened, newest first. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")   // tenant-wide admins: the log shows changes across every unit
    @Operation(operationId = "listAuditEntries",
            summary = "What changed, who changed it and when, newest first; optionally for one user, person or unit")
    AuditPage list(@RequestParam(required = false) @Nullable UUID subjectId,
                   @RequestParam(required = false) @Nullable String after,
                   @RequestParam(defaultValue = "50") int size) {
        return auditTrail.list(new AuditSearch(subjectId, after, size));
    }
}
