package com.healthcare.ordermanagement.pattern.observer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * CHANGE 2a — Wires CompositeNotificationService as the @Primary bean.
 *
 * Spring injects the composite wherever NotificationService is declared
 * (e.g. OrderManager, command classes). Zero changes to those files.
 */
@Configuration
public class NotificationConfig {

    @Bean
    @Primary
    public NotificationService compositeNotificationService(
            ConsoleNotificationService console,
            InAppNotificationService   inApp,
            EmailNotificationService   email,
            NotificationPreferences    preferences) {

        return new CompositeNotificationService(console, inApp, email, preferences);
    }
}