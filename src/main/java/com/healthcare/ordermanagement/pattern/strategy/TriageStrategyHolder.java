package com.healthcare.ordermanagement.pattern.strategy;

import com.healthcare.ordermanagement.domain.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CHANGE 1 — Department-Aware Triage.
 *
 * Implements TriageStrategy and is annotated @Primary so Spring injects THIS
 * bean wherever TriageStrategy is needed (including TriagingEngine).
 * TriagingEngine.java is therefore NEVER modified.
 *
 * At runtime, TriageStrategyController calls setStrategy() to swap the active
 * algorithm without restarting the server.
 */
@Component
@Primary
public class TriageStrategyHolder implements TriageStrategy {

    private volatile TriageStrategy active;

    // Constructor types PriorityTriageStrategy concretely to avoid
    // Spring injecting TriageStrategyHolder into itself (circular dep).
    @Autowired
    public TriageStrategyHolder(PriorityTriageStrategy defaultStrategy) {
        this.active = defaultStrategy;
    }

    // Convenience constructor for unit tests (no Spring context required)
    public TriageStrategyHolder(TriageStrategy strategy) {
        this.active = strategy;
    }

    @Override
    public int determinePosition(List<Order> currentQueue, Order newOrder) {
        return active.determinePosition(currentQueue, newOrder);
    }

    public void setStrategy(TriageStrategy strategy) {
        this.active = strategy;
    }

    public TriageStrategy getActive() { return active; }

    public String getActiveName() {
        return switch (active.getClass().getSimpleName()) {
            case "PriorityTriageStrategy"      -> "PRIORITY";
            case "LoadBalancingTriageStrategy"  -> "LOAD_BALANCE";
            case "DeadlineFirstTriageStrategy"  -> "DEADLINE";
            default                             -> active.getClass().getSimpleName().toUpperCase();
        };
    }
}