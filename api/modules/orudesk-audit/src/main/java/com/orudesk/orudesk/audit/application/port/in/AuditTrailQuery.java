package com.orudesk.orudesk.audit.application.port.in;

public interface AuditTrailQuery {

    AuditPage list(AuditSearch search);
}
