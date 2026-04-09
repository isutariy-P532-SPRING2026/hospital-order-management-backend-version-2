package com.healthcare.ordermanagement.pattern.observer;

import com.healthcare.ordermanagement.domain.Order;

/**
 * CHANGE 2a — Composite channel that fans out to all active channels.
 *
 * OrderManager calls notify() on this composite (injected as @Primary).
 * It checks NotificationPreferences before delegating to each channel.
 * Adding or removing channels requires zero changes to OrderManager.
 */
public class CompositeNotificationService implements NotificationService {

    private final ConsoleNotificationService console;
    private final InAppNotificationService   inApp;
    private final EmailNotificationService   email;
    private final NotificationPreferences    preferences;

    public CompositeNotificationService(ConsoleNotificationService console,
                                        InAppNotificationService   inApp,
                                        EmailNotificationService   email,
                                        NotificationPreferences    preferences) {
        this.console     = console;
        this.inApp       = inApp;
        this.email       = email;
        this.preferences = preferences;
    }

    @Override
    public void notify(Order order, String event) {
        if (preferences.isConsoleEnabled()) console.notify(order, event);
        if (preferences.isInAppEnabled())   inApp.notify(order, event);
        if (preferences.isEmailEnabled())   email.notify(order, event);
    }
}