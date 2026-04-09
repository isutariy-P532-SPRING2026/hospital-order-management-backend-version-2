package com.healthcare.ordermanagement.strategy;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.strategy.LoadBalancingTriageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadBalancingTriageStrategyTest {

    private LoadBalancingTriageStrategy strategy;

    @BeforeEach
    void setUp() { strategy = new LoadBalancingTriageStrategy(); }

    @Test
    void emptyQueue_returnsZero() {
        // Arrange
        List<Order> queue = new ArrayList<>();
        // Act
        int pos = strategy.determinePosition(queue, makeOrder(Priority.STAT));
        // Assert
        assertEquals(0, pos);
    }

    @Test
    void statOrder_goesToEnd_ignoringPriority() {
        // Arrange
        List<Order> queue = List.of(makeOrder(Priority.ROUTINE), makeOrder(Priority.URGENT));
        // Act
        int pos = strategy.determinePosition(new ArrayList<>(queue), makeOrder(Priority.STAT));
        // Assert — FIFO, STAT is not privileged
        assertEquals(2, pos);
    }

    @Test
    void alwaysAppends_multipleOrders() {
        // Arrange
        List<Order> queue = new ArrayList<>();
        for (int i = 0; i < 5; i++) queue.add(makeOrder(Priority.ROUTINE));
        // Act
        int pos = strategy.determinePosition(queue, makeOrder(Priority.STAT));
        // Assert
        assertEquals(5, pos);
    }

    private Order makeOrder(Priority p) {
        return new Order("T-001", OrderType.LAB, "Patient", "Dr", "desc", p);
    }
}