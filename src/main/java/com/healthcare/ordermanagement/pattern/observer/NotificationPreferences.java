package com.healthcare.ordermanagement.pattern.observer;

import org.springframework.stereotype.Component;

/**
 * CHANGE 2a — Holds per-session channel preferences (all enabled by default).
 * Updated via NotificationPreferenceController; read by CompositeNotificationService.
 */
@Component
public class NotificationPreferences {

    private volatile boolean consoleEnabled = true;
    private volatile boolean inAppEnabled   = true;
    private volatile boolean emailEnabled   = true;

    public boolean isConsoleEnabled() { return consoleEnabled; }
    public boolean isInAppEnabled()   { return inAppEnabled;   }
    public boolean isEmailEnabled()   { return emailEnabled;   }

    public void setConsoleEnabled(boolean v) { consoleEnabled = v; }
    public void setInAppEnabled(boolean v)   { inAppEnabled   = v; }
    public void setEmailEnabled(boolean v)   { emailEnabled   = v; }
}