package com.orudesk.orudesk.shared.adapter.out.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * "What time is it?" as a bean. Code that asks the Clock instead of calling LocalDate.now() directly
 * can be tested with a fixed date.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
