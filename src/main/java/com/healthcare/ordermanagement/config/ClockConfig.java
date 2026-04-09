package com.healthcare.ordermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * CHANGE 2b — Provides a Clock bean for injection into time-dependent
 * components (TimedPriorityBoostingDecorator, PriorityEscalationDecorator,
 * DeadlineFirstTriageStrategy).  Tests substitute Clock.fixed() for
 * deterministic assertions.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}