package com.healthcare.ordermanagement.command;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderStatus;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.command.*;
import com.healthcare.ordermanagement.pattern.observer.NotificationService;
import com.healthcare.ordermanagement.resource.InMemoryOrderAccess;
import com.healthcare.ordermanagement.resource.OrderAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CommandReplayTest {

    @Mock NotificationService notificationService;

    private OrderAccess orderAccess;
    private CommandLog  commandLog;
    private Order       order;

    @BeforeEach
    void setUp() {
        orderAccess = new InMemoryOrderAccess();
        commandLog  = new CommandLog();
        order = new Order("LAB-001", OrderType.LAB, "Alice", "Dr. A", "CBC", Priority.ROUTINE);
        orderAccess.saveOrder(order);
    }

    @Test
    void undoCancel_restoresOrderToPending() {
        // Arrange
        order.setStatus(OrderStatus.PENDING);
        Command cancel = new CancelOrderCommand("LAB-001", "Dr. A", orderAccess, notificationService);
        cancel.execute();
        commandLog.record(cancel);
        assertEquals(OrderStatus.CANCELLED, orderAccess.findOrderById("LAB-001").getStatus());

        // Act
        boolean undone = commandLog.undoLast();

        // Assert
        assertTrue(undone);
        assertEquals(OrderStatus.PENDING, orderAccess.findOrderById("LAB-001").getStatus());
    }

    @Test
    void replayAt_validIndex_returnsCommandAndRecordsEntry() {
        // Arrange
        Command claim = new ClaimOrderCommand("LAB-001", "Nurse Kim", orderAccess, notificationService);
        claim.execute();
        commandLog.record(claim);
        int sizeBefore = commandLog.getAll().size();

        // Reset state for replay
        order.setStatus(OrderStatus.PENDING);
        order.setClaimedBy(null);
        orderAccess.saveOrder(order);

        // Act
        Command replayed = commandLog.replayAt(0);

        // Assert
        assertNotNull(replayed);
        assertEquals(sizeBefore + 1, commandLog.getAll().size()); // replay entry added
        assertEquals("REPLAY:CLAIM", commandLog.getAll().get(commandLog.getAll().size() - 1).getCommandType());
    }

    @Test
    void replayAt_invalidIndex_returnsNull() {
        // Arrange / Act
        Command result = commandLog.replayAt(99);
        // Assert
        assertNull(result);
    }

    @Test
    void undoLastAndGet_returnsNullWhenStackEmpty() {
        // Arrange — empty log
        // Act
        Command result = commandLog.undoLastAndGet();
        // Assert
        assertNull(result);
    }

    @Test
    void recordNote_appearsInLog_notInUndoStack() {
        // Arrange
        commandLog.recordNote("STAT_AUDIT", "LAB-001", "System: info");
        // Act — undo should have nothing to undo
        boolean canUndo = commandLog.undoLast();
        // Assert
        assertEquals(1, commandLog.getAll().size());
        assertFalse(canUndo); // recordNote not on undo stack
    }
}