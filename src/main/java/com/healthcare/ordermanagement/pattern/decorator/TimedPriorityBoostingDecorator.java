package com.healthcare.ordermanagement.pattern.decorator;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.Priority;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * CHANGE 2b — Clock-injected replacement for PriorityBoostingDecorator.
 *
 * The original PriorityBoostingDecorator is untouched (zero modifications).
 * OrderHandlerFactory now uses THIS class in the chain instead.
 *
 * Boosts URGENT orders that have been waiting >= 30 minutes to STAT.
 */
public class TimedPriorityBoostingDecorator implements OrderHandler {

    private static final long BOOST_THRESHOLD_MINUTES = 30;

    private final OrderHandler wrapped;
    private final Clock        clock;

    public TimedPriorityBoostingDecorator(OrderHandler wrapped, Clock clock) {
        this.wrapped = wrapped;
        this.clock   = clock;
    }

    @Override
    public void handle(Order order) {
        boost(order);
        wrapped.handle(order);
    }

    private void boost(Order order) {
        if (order.getPriority() != Priority.URGENT) return;

        long waitMinutes = Duration.between(
            order.getSubmittedAt(), LocalDateTime.now(clock)
        ).toMinutes();

        if (waitMinutes >= BOOST_THRESHOLD_MINUTES) {
            order.setPriority(Priority.STAT);
            System.out.printf(
                "  [PRIORITY BOOST] Order %s boosted URGENT → STAT after %d min wait%n",
                order.getOrderId(), waitMinutes
            );
        }
    }
}