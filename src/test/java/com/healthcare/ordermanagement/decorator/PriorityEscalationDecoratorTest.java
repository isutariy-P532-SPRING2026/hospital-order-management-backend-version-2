package com.healthcare.ordermanagement.decorator;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.decorator.PriorityEscalationDecorator;
import com.healthcare.ordermanagement.resource.InMemoryOrderAccess;
import com.healthcare.ordermanagement.resource.OrderAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityEscalationDecoratorTest {

    private OrderAccess orderAccess;
    private Clock       fixedClock;

    @BeforeEach
    void setUp() {
        orderAccess = new InMemoryOrderAccess();
        fixedClock  = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    @Test
    void urgentEscalated_whenRecentStatOfSameTypeExists() {
        // Arrange — STAT LAB submitted just now
        Order stat = order("S-001", OrderType.LAB, Priority.STAT, LocalDateTime.now(fixedClock));
        orderAccess.saveOrder(stat);
        Order urgent = order("U-001", OrderType.LAB, Priority.URGENT, LocalDateTime.now(fixedClock));

        // Act
        new PriorityEscalationDecorator(o -> {}, orderAccess, fixedClock).handle(urgent);

        // Assert
        assertEquals(Priority.STAT, urgent.getPriority());
    }

    @Test
    void urgentNotEscalated_whenStatIsOlderThanWindow() {
        // Arrange — STAT submitted 10 min ago (outside 5-min window)
        Order stat = order("S-001", OrderType.LAB, Priority.STAT,
                           LocalDateTime.now(fixedClock).minusMinutes(10));
        orderAccess.saveOrder(stat);
        Order urgent = order("U-001", OrderType.LAB, Priority.URGENT, LocalDateTime.now(fixedClock));

        // Act
        new PriorityEscalationDecorator(o -> {}, orderAccess, fixedClock).handle(urgent);

        // Assert
        assertEquals(Priority.URGENT, urgent.getPriority());
    }

    @Test
    void urgentNotEscalated_whenNoStatExists() {
        // Arrange — empty store
        Order urgent = order("U-001", OrderType.LAB, Priority.URGENT, LocalDateTime.now(fixedClock));

        // Act
        new PriorityEscalationDecorator(o -> {}, orderAccess, fixedClock).handle(urgent);

        // Assert
        assertEquals(Priority.URGENT, urgent.getPriority());
    }

    @Test
    void routineNotAffected_evenWhenStatExists() {
        // Arrange
        Order stat = order("S-001", OrderType.LAB, Priority.STAT, LocalDateTime.now(fixedClock));
        orderAccess.saveOrder(stat);
        Order routine = order("R-001", OrderType.LAB, Priority.ROUTINE, LocalDateTime.now(fixedClock));

        // Act
        new PriorityEscalationDecorator(o -> {}, orderAccess, fixedClock).handle(routine);

        // Assert — only URGENT is eligible for escalation
        assertEquals(Priority.ROUTINE, routine.getPriority());
    }

    @Test
    void urgentNotEscalated_whenStatIsOfDifferentType() {
        // Arrange — STAT IMAGING, but new order is LAB URGENT
        Order stat = order("S-001", OrderType.IMAGING, Priority.STAT, LocalDateTime.now(fixedClock));
        orderAccess.saveOrder(stat);
        Order urgent = order("U-001", OrderType.LAB, Priority.URGENT, LocalDateTime.now(fixedClock));

        // Act
        new PriorityEscalationDecorator(o -> {}, orderAccess, fixedClock).handle(urgent);

        // Assert — different type, no escalation
        assertEquals(Priority.URGENT, urgent.getPriority());
    }

    private Order order(String id, OrderType type, Priority p, LocalDateTime submittedAt) {
        Order o = new Order(id, type, "Patient", "Dr", "desc", p);
        o.setSubmittedAt(submittedAt);
        return o;
    }
}