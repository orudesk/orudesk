/**
 * Audit feature: an append-only record of who changed what. Other modules never call it: they publish
 * {@link com.orudesk.orudesk.shared.AuditableEvent}s, and this module listens.
 */
package com.orudesk.orudesk.audit;
