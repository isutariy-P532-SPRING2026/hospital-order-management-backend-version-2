package com.healthcare.ordermanagement.pattern.observer;

import com.healthcare.ordermanagement.domain.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CHANGE 2a — Email notification channel (mock: formats and prints to console).
 * In production this would delegate to an SMTP or SES client.
 */
@Component
public class EmailNotificationService implements NotificationService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINE =
            "  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─";

    @Override
    public void notify(Order order, String event) {
        System.out.println();
        System.out.println(LINE);
        System.out.printf("  [EMAIL MOCK]  Sent at %s%n", FMT.format(LocalDateTime.now()));
        System.out.printf("  To      : %s%n", resolveRecipient(order, event));
        System.out.printf("  Subject : Order %s — %s%n", order.getOrderId(), event.replace('_', ' '));
        System.out.printf("  Body    : Patient: %-20s | Type: %-10s | Priority: %-8s | Status: %s%n",
            order.getPatientName(), order.getType(), order.getPriority(), order.getStatus());
        System.out.println(LINE);
        System.out.println();
    }

    private String resolveRecipient(Order order, String event) {
        return switch (event) {
            case "ORDER_SUBMITTED"  -> "fulfilment@hospital.org";
            case "ORDER_CLAIMED"    -> toEmail(order.getClinicianName());
            case "ORDER_COMPLETED"  -> toEmail(order.getClinicianName());
            case "ORDER_CANCELLED"  -> "fulfilment@hospital.org";
            default                 -> "admin@hospital.org";
        };
    }

    private String toEmail(String name) {
        return name.toLowerCase().replace(" ", ".").replace("dr.", "dr") + "@hospital.org";
    }
}