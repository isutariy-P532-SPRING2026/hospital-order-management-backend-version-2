package com.healthcare.ordermanagement.pattern.observer;

import com.healthcare.ordermanagement.domain.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ConsoleNotificationService implements NotificationService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINE =
            "  ════════════════════════════════════════════════════════════════════";

    @Override
    public void notify(Order order, String event) {
        String recipient = resolveRecipient(order, event);
        String change    = resolveChange(event, order);

        System.out.println();
        System.out.println(LINE);
        System.out.printf("  ║  CONSOLE NOTIFICATION  │  %-30s  │  %s%n",
                event, FMT.format(LocalDateTime.now()));
        System.out.println(LINE);
        System.out.printf("  ║  Order ID  : %-20s  │  Type     : %s%n",
                order.getOrderId(), order.getType());
        System.out.printf("  ║  Patient   : %-20s  │  Priority : %s%n",
                order.getPatientName(), order.getPriority());
        System.out.printf("  ║  Clinician : %-20s  │  Status   : %s%n",
                order.getClinicianName(), order.getStatus());
        System.out.printf("  ║  Notified  : %s%n", recipient);
        System.out.printf("  ║  Change    : %s%n", change);
        System.out.println(LINE);
        System.out.println();
    }

    private String resolveRecipient(Order order, String event) {
        return switch (event) {
            case "ORDER_SUBMITTED"           -> "Fulfilment Department";
            case "ORDER_CLAIMED"             -> order.getClinicianName();
            case "ORDER_COMPLETED"           -> order.getClinicianName() + " & " + order.getPatientName();
            case "ORDER_CANCELLED"           -> "Fulfilment Department";
            case "ORDER_SUBMISSION_UNDONE"   -> "Fulfilment Department";
            case "ORDER_CLAIM_UNDONE"        -> order.getClinicianName();
            case "ORDER_COMPLETION_UNDONE"   -> order.getClinicianName();
            case "ORDER_CANCELLATION_UNDONE" -> "Fulfilment Department";
            default                          -> "System";
        };
    }

    private String resolveChange(String event, Order order) {
        return switch (event) {
            case "ORDER_SUBMITTED"           -> "New order queued  →  Status: PENDING";
            case "ORDER_CLAIMED"             -> "Claimed by " + order.getClaimedBy() + "  →  Status: IN_PROGRESS";
            case "ORDER_COMPLETED"           -> "Completed by " + order.getClaimedBy() + "  →  Status: COMPLETED";
            case "ORDER_CANCELLED"           -> "Cancelled  →  Status: CANCELLED";
            case "ORDER_SUBMISSION_UNDONE"   -> "Submission reversed  →  Order removed from system";
            case "ORDER_CLAIM_UNDONE"        -> "Claim released  →  Status: PENDING (back in queue)";
            case "ORDER_COMPLETION_UNDONE"   -> "Completion reversed  →  Status: IN_PROGRESS";
            case "ORDER_CANCELLATION_UNDONE" -> "Cancellation reversed  →  Status: PENDING (back in queue)";
            default                          -> "Status: " + order.getStatus();
        };
    }
}