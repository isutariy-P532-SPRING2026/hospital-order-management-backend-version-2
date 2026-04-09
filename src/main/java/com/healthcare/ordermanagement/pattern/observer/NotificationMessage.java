package com.healthcare.ordermanagement.pattern.observer;

import lombok.Getter;
import java.time.LocalDateTime;

/**
 * CHANGE 2a — A single in-app notification message stored by InAppNotificationService.
 */
@Getter
public class NotificationMessage {

    private final String        id;
    private final String        event;
    private final String        text;
    private final String        orderId;   // null for system messages (e.g. strategy change)
    private final LocalDateTime timestamp;

    public NotificationMessage(String id, String event, String text,
                               String orderId, LocalDateTime timestamp) {
        this.id        = id;
        this.event     = event;
        this.text      = text;
        this.orderId   = orderId;
        this.timestamp = timestamp;
    }
}