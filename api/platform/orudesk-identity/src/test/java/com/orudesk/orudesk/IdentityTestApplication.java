package com.orudesk.orudesk;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Only for this module's slice tests (@WebMvcTest, @DataJpaTest). They need a @SpringBootConfiguration
 * to start from, and the real one (OrudeskApplication) lives in the app module, which identity can't see.
 * Same package as the real one, so it finds the same beans: identity's and platform-core's.
 */
@SpringBootApplication
class IdentityTestApplication {
}
