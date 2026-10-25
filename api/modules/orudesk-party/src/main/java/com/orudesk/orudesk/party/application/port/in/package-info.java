/**
 * The party module's public API: the only package other modules and verticals may use.
 * Its types are plain Java (UUID, String, LocalDate), so callers never depend on party's domain classes.
 */
@NamedInterface("api")
package com.orudesk.orudesk.party.application.port.in;

import org.springframework.modulith.NamedInterface;
