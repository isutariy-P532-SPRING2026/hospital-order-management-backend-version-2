package com.healthcare.ordermanagement.strategy;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.strategy.DeadlineFirstTriageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeadlineFirstTriageStrategyTest {

    private DeadlineFirstTriageStrategy strategy;
    private Clock fixedClock;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        now        = LocalDateTime.now(fixedClock);
        strategy   = new DeadlineFirstTriageStrategy(fixedClock);
    }

    @Test
    void emptyQueue_returnsZero() {
        // Arrange / Act
        int pos = strategy.determinePosition(new ArrayList<>(), order(Priority.ROUTINE, now));
        // Assert
        assertEquals(0, pos);
    }

    @Test
    void statBeforeUrgentBeforeRoutine_whenAllSubmittedNow() {
        // Arrange — all submitted now; STAT has least time remaining (30 min)
        List<Order> queue = new ArrayList<>();
        queue.add(order(Priority.URGENT,  now)); // 120 min remaining
        queue.add(order(Priority.ROUTINE, now)); // 480 min remaining
        // Act
        int pos = strategy.determinePosition(queue, order(Priority.STAT, now));
        // Assert — STAT goes before URGENT
        assertEquals(0, pos);
    }

    @Test
    void olderStatBeforeNewerStat() {
        // Arrange
        List<Order> queue = new ArrayList<>();
        // Old STAT (20 min ago) → 10 min remaining
        queue.add(order(Priority.STAT, now.minusMinutes(20)));
        // New STAT (just submitted) → 30 min remaining
        Order newStat = order(Priority.STAT, now);
        // Act
        int pos = strategy.determinePosition(queue, newStat);
        // Assert — old stat expires sooner, new stat goes after
        assertEquals(1, pos);
    }

    @Test
    void urgentGoesAfterStatBeforeRoutine() {
        // Arrange
        List<Order> queue = new ArrayList<>();
        queue.add(order(Priority.STAT,    now)); // 30 min
        queue.add(order(Priority.ROUTINE, now)); // 480 min
        // Act
        int pos = strategy.determinePosition(queue, order(Priority.URGENT, now)); // 120 min
        // Assert
        assertEquals(1, pos);
    }

    private Order order(Priority p, LocalDateTime submittedAt) {
        Order o = new Order("T-001", OrderType.LAB, "Patient", "Dr", "desc", p);
        o.setSubmittedAt(submittedAt);
        return o;
    }
}