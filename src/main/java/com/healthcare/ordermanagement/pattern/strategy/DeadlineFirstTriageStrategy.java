package com.healthcare.ordermanagement.pattern.strategy;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.Priority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CHANGE 1 — Deadline-First Triage Strategy.
 *
 * Deadlines per priority:  STAT = 30 min | URGENT = 120 min | ROUTINE = 480 min.
 * Orders with less time remaining go to the front (soonest-to-expire first).
 * Clock is injected so tests can use a fixed instant (deterministic).
 */
@Component
public class DeadlineFirstTriageStrategy implements TriageStrategy {

    static final long STAT_MINUTES    =  30;
    static final long URGENT_MINUTES  = 120;
    static final long ROUTINE_MINUTES = 480;

    private final Clock clock;

    @Autowired
    public DeadlineFirstTriageStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public int determinePosition(List<Order> currentQueue, Order newOrder) {
        long newRemaining = minutesRemaining(newOrder);
        for (int i = 0; i < currentQueue.size(); i++) {
            if (newRemaining < minutesRemaining(currentQueue.get(i))) return i;
        }
        return currentQueue.size();
    }

    private long minutesRemaining(Order order) {
        long deadline = deadlineMinutes(order.getPriority());
        LocalDateTime expires = order.getSubmittedAt().plusMinutes(deadline);
        return Duration.between(LocalDateTime.now(clock), expires).toMinutes();
    }

    private long deadlineMinutes(Priority p) {
        return switch (p) {
            case STAT    -> STAT_MINUTES;
            case URGENT  -> URGENT_MINUTES;
            case ROUTINE -> ROUTINE_MINUTES;
        };
    }
}