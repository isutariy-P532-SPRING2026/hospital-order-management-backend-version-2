package com.healthcare.ordermanagement.pattern.strategy;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderStatus;
import com.healthcare.ordermanagement.resource.OrderAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CHANGE 1 — Load-Balancing Triage Strategy.
 *
 * Goal: the staff member with the fewest IN_PROGRESS orders claims next,
 * regardless of priority.
 *
 * How it works at enqueue time:
 *   1. Count how many IN_PROGRESS orders each staff member currently holds.
 *   2. Find the minimum load across all active staff.
 *   3. Walk the current PENDING queue; insert the new order immediately
 *      before the first order whose "assigned staff" (claimedBy on the
 *      first IN_PROGRESS order of that staff) has a load HIGHER than the
 *      minimum — so the least-busy staff member will reach it soonest.
 *   4. If no such position exists, append to end (pure FIFO fallback when
 *      all staff are equally loaded or no staff are active yet).
 *
 * Because fulfilment staff claim the FRONT of the queue, inserting near
 * the front routes work toward under-loaded staff without hard-assigning.
 */
@Component
public class LoadBalancingTriageStrategy implements TriageStrategy {

    private final OrderAccess orderAccess;

    @Autowired
    public LoadBalancingTriageStrategy(OrderAccess orderAccess) {
        this.orderAccess = orderAccess;
    }

    @Override
    public int determinePosition(List<Order> currentQueue, Order newOrder) {

        // ── Step 1: build staff → in-progress count map ───────────────────
        Map<String, Long> staffLoad = orderAccess.listAllOrders().stream()
            .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS)
            .filter(o -> o.getClaimedBy() != null)
            .collect(Collectors.groupingBy(Order::getClaimedBy, Collectors.counting()));

        // No staff active at all → pure FIFO, append to end
        if (staffLoad.isEmpty()) {
            return currentQueue.size();
        }

        // ── Step 2: find minimum load ─────────────────────────────────────
        long minLoad = staffLoad.values().stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0L);

        // ── Step 3: find the first staff member at minimum load ───────────
        String leastBusyStaff = staffLoad.entrySet().stream()
            .filter(e -> e.getValue() == minLoad)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

        // ── Step 4: insert before the first order held by a MORE-loaded
        //            staff member, so the least-busy staff reaches it first.
        //            If the queue has orders already claimed by the
        //            least-busy staff, respect FIFO among those. ───────────
        for (int i = 0; i < currentQueue.size(); i++) {
            String claimedBy = currentQueue.get(i).getClaimedBy();
            if (claimedBy != null && !claimedBy.equals(leastBusyStaff)) {
                long load = staffLoad.getOrDefault(claimedBy, 0L);
                if (load > minLoad) {
                    return i; // insert here — less-loaded staff gets it sooner
                }
            }
        }

        // Fallback: equal load or no match → append (FIFO)
        return currentQueue.size();
    }
}