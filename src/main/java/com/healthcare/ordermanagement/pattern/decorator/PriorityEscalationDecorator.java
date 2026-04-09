package com.healthcare.ordermanagement.pattern.decorator;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.resource.OrderAccess;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CHANGE 2b — Priority Escalation Decorator.
 *
 * If a new URGENT order is submitted within WINDOW_MINUTES of any existing
 * STAT order of the same type, the new order is escalated to STAT.
 *
 * Runs AFTER TimedPriorityBoostingDecorator, BEFORE StatAuditDecorator,
 * so StatAuditDecorator always sees the final resolved priority.
 */
public class PriorityEscalationDecorator implements OrderHandler {

    static final long WINDOW_MINUTES = 5;

    private final OrderHandler wrapped;
    private final OrderAccess  orderAccess;
    private final Clock        clock;

    public PriorityEscalationDecorator(OrderHandler wrapped,
                                       OrderAccess orderAccess,
                                       Clock clock) {
        this.wrapped     = wrapped;
        this.orderAccess = orderAccess;
        this.clock       = clock;
    }

    @Override
    public void handle(Order order) {
        escalate(order);
        wrapped.handle(order);
    }

    private void escalate(Order order) {
        if (order.getPriority() != Priority.URGENT) return;

        LocalDateTime windowStart = LocalDateTime.now(clock).minusMinutes(WINDOW_MINUTES);

        List<Order> recentStat = orderAccess.listAllOrders().stream()
            .filter(o -> o.getType()     == order.getType())
            .filter(o -> o.getPriority() == Priority.STAT)
            .filter(o -> o.getSubmittedAt().isAfter(windowStart))
            .toList();

        if (!recentStat.isEmpty()) {
            order.setPriority(Priority.STAT);
            System.out.printf(
                "  [ESCALATION] Order %s escalated URGENT → STAT " +
                "(recent STAT %s within %d-min window)%n",
                order.getOrderId(), order.getType(), WINDOW_MINUTES
            );
        }
    }
}