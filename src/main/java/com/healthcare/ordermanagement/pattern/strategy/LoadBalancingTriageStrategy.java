package com.healthcare.ordermanagement.pattern.strategy;

import com.healthcare.ordermanagement.domain.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CHANGE 1 — Load-Balancing Triage Strategy.
 *
 * Pure FIFO — every new order goes to the end of the queue regardless of
 * priority.  Ensures equitable throughput across all fulfilment staff.
 */
@Component
public class LoadBalancingTriageStrategy implements TriageStrategy {

    @Override
    public int determinePosition(List<Order> currentQueue, Order newOrder) {
        return currentQueue.size(); // always append
    }
}