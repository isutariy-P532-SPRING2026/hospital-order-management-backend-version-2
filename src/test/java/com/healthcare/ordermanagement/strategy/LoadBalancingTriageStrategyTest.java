package com.healthcare.ordermanagement.strategy;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderStatus;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.strategy.LoadBalancingTriageStrategy;
import com.healthcare.ordermanagement.resource.InMemoryOrderAccess;
import com.healthcare.ordermanagement.resource.OrderAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadBalancingTriageStrategyTest {

    private OrderAccess               orderAccess;
    private LoadBalancingTriageStrategy strategy;

    @BeforeEach
    void setUp() {
        orderAccess = new InMemoryOrderAccess();
        strategy    = new LoadBalancingTriageStrategy(orderAccess);
    }

    @Test
    void noStaffActive_appendsToEnd_fifo() {
        // Arrange — no IN_PROGRESS orders, no staff load data
        List<Order> queue = new ArrayList<>();
        queue.add(makeOrder("R-001", Priority.ROUTINE));
        queue.add(makeOrder("R-002", Priority.ROUTINE));
        // Act
        int pos = strategy.determinePosition(queue, makeOrder("NEW", Priority.STAT));
        // Assert — no staff info → pure FIFO fallback
        assertEquals(2, pos);
    }

    @Test
    void emptyQueue_returnsZero() {
        // Arrange / Act
        int pos = strategy.determinePosition(new ArrayList<>(), makeOrder("NEW", Priority.ROUTINE));
        // Assert
        assertEquals(0, pos);
    }

    @Test
    void leastLoadedStaffOrderGoesFirst() {
        // Arrange — Nurse A has 2 in-progress, Nurse B has 1
        Order a1 = inProgress("A-001", "Nurse A");
        Order a2 = inProgress("A-002", "Nurse A");
        Order b1 = inProgress("B-001", "Nurse B");
        orderAccess.saveOrder(a1);
        orderAccess.saveOrder(a2);
        orderAccess.saveOrder(b1);

        // Queue already has one PENDING order tagged as going to Nurse A's direction
        List<Order> queue = new ArrayList<>();
        Order existing = makeOrder("P-001", Priority.ROUTINE);
        existing.setClaimedBy("Nurse A"); // this pending order is "headed" to busy Nurse A
        queue.add(existing);

        // Act — new order should be inserted BEFORE the Nurse A order (Nurse B is less loaded)
        int pos = strategy.determinePosition(queue, makeOrder("NEW", Priority.URGENT));

        // Assert — new order inserted before the Nurse A-bound order
        assertEquals(0, pos);
    }

    @Test
    void equalLoad_appendsToEndFifo() {
        // Arrange — all staff equally loaded
        orderAccess.saveOrder(inProgress("A-001", "Nurse A"));
        orderAccess.saveOrder(inProgress("B-001", "Nurse B"));

        List<Order> queue = new ArrayList<>();
        queue.add(makeOrder("P-001", Priority.ROUTINE));

        // Act
        int pos = strategy.determinePosition(queue, makeOrder("NEW", Priority.ROUTINE));

        // Assert — equal load → FIFO fallback, go to end
        assertEquals(1, pos);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Order makeOrder(String id, Priority p) {
        return new Order(id, OrderType.LAB, "Patient", "Dr", "desc", p);
    }

    private Order inProgress(String id, String claimedBy) {
        Order o = new Order(id, OrderType.LAB, "Patient", "Dr", "desc", Priority.ROUTINE);
        o.setStatus(OrderStatus.IN_PROGRESS);
        o.setClaimedBy(claimedBy);
        return o;
    }
}