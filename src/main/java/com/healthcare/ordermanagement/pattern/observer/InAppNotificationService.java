package com.healthcare.ordermanagement.pattern.observer;

import com.healthcare.ordermanagement.domain.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CHANGE 2a — In-app notification channel.
 *
 * Stores a full list of NotificationMessage objects (newest first).
 * The UI polls GET /api/notifications/messages for the list and shows
 * an unread badge from GET /api/notifications/badge.
 *
 * Also provides notifySystem() for non-order events (e.g. strategy change).
 */
@Component
public class InAppNotificationService implements NotificationService {

    private final CopyOnWriteArrayList<NotificationMessage> messages = new CopyOnWriteArrayList<>();
    private final AtomicInteger unreadCount = new AtomicInteger(0);

    @Override
    public void notify(Order order, String event) {
        String text = buildText(order, event);
        messages.add(0, new NotificationMessage(
            UUID.randomUUID().toString(), event, text,
            order.getOrderId(), LocalDateTime.now()
        ));
        unreadCount.incrementAndGet();
    }

    // For system events that have no associated Order (e.g. strategy change)
    public void notifySystem(String event, String text) {
        messages.add(0, new NotificationMessage(
            UUID.randomUUID().toString(), event, text, null, LocalDateTime.now()
        ));
        unreadCount.incrementAndGet();
    }

    public List<NotificationMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int getBadgeCount() { return unreadCount.get(); }

    public void clearAll() {
        messages.clear();
        unreadCount.set(0);
    }

    public boolean deleteMessage(String id) {
        boolean removed = messages.removeIf(m -> m.getId().equals(id));
        if (removed && unreadCount.get() > 0) unreadCount.decrementAndGet();
        return removed;
    }

    public void markAllRead() { unreadCount.set(0); }

    private String buildText(Order order, String event) {
        return switch (event) {
            case "ORDER_SUBMITTED"           -> "New order queued: " + order.getOrderId()
                + " | Patient: " + order.getPatientName()
                + " | Priority: " + order.getPriority();
            case "ORDER_CLAIMED"             -> "Order " + order.getOrderId()
                + " claimed by " + order.getClaimedBy();
            case "ORDER_COMPLETED"           -> "Order " + order.getOrderId()
                + " completed for " + order.getPatientName();
            case "ORDER_CANCELLED"           -> "Order " + order.getOrderId()
                + " cancelled by " + order.getClinicianName();
            case "ORDER_SUBMISSION_UNDONE"   -> "Submission undone: " + order.getOrderId() + " removed";
            case "ORDER_CLAIM_UNDONE"        -> "Claim undone: " + order.getOrderId() + " back in queue";
            case "ORDER_COMPLETION_UNDONE"   -> "Completion undone: " + order.getOrderId() + " back in progress";
            case "ORDER_CANCELLATION_UNDONE" -> "Cancellation undone: " + order.getOrderId() + " back in queue";
            default                          -> event + " — Order: " + order.getOrderId();
        };
    }
}