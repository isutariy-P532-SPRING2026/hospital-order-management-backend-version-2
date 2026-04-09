package com.healthcare.ordermanagement.pattern.decorator;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.command.CommandLog;
import com.healthcare.ordermanagement.resource.OrderAccess;

/**
 * CHANGE 2b — STAT Audit Decorator.
 *
 * When a STAT order passes through (either originally STAT or escalated),
 * records a STAT_AUDIT entry directly into the CommandLog so it appears
 * in the audit trail visible in the UI.
 *
 * Records: affected patient count (distinct STAT patients), and whether
 * this order was already STAT at submission vs escalated.
 * Runs AFTER PriorityEscalationDecorator so it sees the final priority.
 */
public class StatAuditDecorator implements OrderHandler {

    private final OrderHandler wrapped;
    private final OrderAccess  orderAccess;
    private final CommandLog   commandLog;

    public StatAuditDecorator(OrderHandler wrapped,
                              OrderAccess orderAccess,
                              CommandLog commandLog) {
        this.wrapped     = wrapped;
        this.orderAccess = orderAccess;
        this.commandLog  = commandLog;
    }

    @Override
    public void handle(Order order) {
        if (order.getPriority() == Priority.STAT) {
            recordStatAudit(order);
        }
        wrapped.handle(order);
    }

    private void recordStatAudit(Order order) {
        // Count distinct patients currently with STAT orders (excluding this new one)
        long statPatients = orderAccess.listAllOrders().stream()
            .filter(o -> o.getPriority() == Priority.STAT)
            .map(Order::getPatientName)
            .distinct()
            .count();

        long statOrders = orderAccess.listAllOrders().stream()
            .filter(o -> o.getPriority() == Priority.STAT)
            .count();

        String actor = String.format(
            "Patient:%s | ExistingSTAT-orders:%d | ExistingSTAT-patients:%d | Type:%s",
            order.getPatientName(), statOrders, statPatients, order.getType()
        );

        // Visible in the UI audit trail as a STAT_AUDIT row
        commandLog.recordNote("STAT_AUDIT", order.getOrderId(), actor);

        System.out.printf(
            "  [STAT AUDIT] Order %s | Patient: %s | " +
            "Existing STAT orders: %d | Existing STAT patients: %d%n",
            order.getOrderId(), order.getPatientName(), statOrders, statPatients
        );
    }
}