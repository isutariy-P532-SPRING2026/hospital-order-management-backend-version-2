package com.healthcare.ordermanagement.manager;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderStatus;
import com.healthcare.ordermanagement.engine.TriagingEngine;
import com.healthcare.ordermanagement.pattern.command.*;
import com.healthcare.ordermanagement.pattern.decorator.OrderHandler;
import com.healthcare.ordermanagement.pattern.decorator.OrderHandlerFactory;
import com.healthcare.ordermanagement.pattern.factory.OrderFactory;
import com.healthcare.ordermanagement.pattern.observer.NotificationService;
import com.healthcare.ordermanagement.resource.OrderAccess;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderManager {

    private final OrderFactory         orderFactory;
    private final OrderAccess          orderAccess;
    private final TriagingEngine       triagingEngine;
    private final NotificationService  notificationService;
    private final CommandLog           commandLog;
    private final OrderHandlerFactory  orderHandlerFactory;

    public OrderManager(OrderFactory orderFactory,
                        OrderAccess orderAccess,
                        TriagingEngine triagingEngine,
                        NotificationService notificationService,
                        CommandLog commandLog,
                        OrderHandlerFactory orderHandlerFactory) {
        this.orderFactory       = orderFactory;
        this.orderAccess        = orderAccess;
        this.triagingEngine     = triagingEngine;
        this.notificationService = notificationService;
        this.commandLog         = commandLog;
        this.orderHandlerFactory = orderHandlerFactory;
    }

    // ── Use Case 1: Submit ────────────────────────────────────────────────────

    public Order submitOrder(String orderType, String patientName,
                             String clinicianName, String description, String priority) {
        Order order = orderFactory.createOrder(orderType, patientName, clinicianName, description, priority);

        OrderHandler chain = orderHandlerFactory.buildChain();
        chain.handle(order);

        triagingEngine.enqueue(order);

        Command command = new SubmitOrderCommand(order, orderAccess, notificationService);
        command.execute();
        commandLog.record(command);

        return order;
    }

    // ── Use Case 2a: Claim ────────────────────────────────────────────────────

    public Order claimNextOrder(String staffName) {
        Order next = triagingEngine.peekNext();
        if (next.getClaimedBy() != null) {
            throw new IllegalStateException(
                "Order " + next.getOrderId() + " is already claimed by " + next.getClaimedBy());
        }
        triagingEngine.dequeue(next.getOrderId());

        Command command = new ClaimOrderCommand(next.getOrderId(), staffName, orderAccess, notificationService);
        command.execute();
        commandLog.record(command);

        return orderAccess.findOrderById(next.getOrderId());
    }

    // ── Use Case 2b: Complete ─────────────────────────────────────────────────

    public Order completeOrder(String orderId, String staffName) {
        Order order = orderAccess.findOrderById(orderId);
        if (!staffName.equals(order.getClaimedBy())) {
            throw new IllegalStateException(staffName + " did not claim order " + orderId);
        }
        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Order " + orderId + " is not in progress — status is " + order.getStatus());
        }
        Command command = new CompleteOrderCommand(orderId, staffName, orderAccess, notificationService);
        command.execute();
        commandLog.record(command);

        return orderAccess.findOrderById(orderId);
    }

    // ── Use Case 3: Cancel ────────────────────────────────────────────────────

    public Order cancelOrder(String orderId, String clinicianName) {
        Order order = orderAccess.findOrderById(orderId);
        if (!clinicianName.equals(order.getClinicianName())) {
            throw new IllegalStateException(clinicianName + " did not submit order " + orderId);
        }
        triagingEngine.dequeue(orderId);

        Command command = new CancelOrderCommand(orderId, clinicianName, orderAccess, notificationService);
        command.execute();
        commandLog.record(command);

        return orderAccess.findOrderById(orderId);
    }

    // ── CHANGE 3: Undo — fixes TriagingEngine queue sync after undo ──────────

    public boolean undoLastCommand() {
        Command undone = commandLog.undoLastAndGet();
        if (undone == null) return false;

        // Sync TriagingEngine with the state change caused by undo
        switch (undone.getCommandType()) {

            case "SUBMIT" -> {
                // Undo of submit deletes the order → remove from engine queue too
                triagingEngine.dequeue(undone.getOrderId());
            }

            case "CLAIM", "CANCEL" -> {
                // Undo of claim/cancel restores order to PENDING → re-add to queue
                try {
                    Order restored = orderAccess.findOrderById(undone.getOrderId());
                    if (restored.getStatus() == OrderStatus.PENDING) {
                        triagingEngine.dequeue(restored.getOrderId()); // avoid duplicates
                        triagingEngine.enqueue(restored);
                    }
                } catch (Exception ignored) {}
            }

            // COMPLETE undo → status goes back to IN_PROGRESS, not PENDING → no queue action
        }
        return true;
    }

    // ── CHANGE 3: Replay ──────────────────────────────────────────────────────

    public boolean replayCommand(int index) {
        Command replayed = commandLog.replayAt(index);
        if (replayed == null) return false;

        // Sync TriagingEngine after replay
        switch (replayed.getCommandType()) {

            case "SUBMIT" -> {
                // Replay of submit re-saves order to OrderAccess → enqueue in engine
                try {
                    Order order = orderAccess.findOrderById(replayed.getOrderId());
                    triagingEngine.dequeue(order.getOrderId()); // avoid duplicates
                    triagingEngine.enqueue(order);
                } catch (Exception ignored) {}
            }

            case "CANCEL" -> {
                // Replay of cancel → order goes to CANCELLED → remove from engine
                triagingEngine.dequeue(replayed.getOrderId());
            }
            // CLAIM replay → order already dequeued before it was claimed originally
            // COMPLETE replay → already not in queue
        }
        return true;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<Order>             getQueue()                      { return triagingEngine.getSortedQueue(); }
    public List<Order>             getAllOrders()                   { return orderAccess.listAllOrders(); }
    public Order                   getOrderById(String id)         { return orderAccess.findOrderById(id); }
    public List<Order>             getOrdersByStatus(OrderStatus s){ return orderAccess.listOrdersByStatus(s); }
    public List<CommandLogEntry>   getCommandLog()                 { return commandLog.getAll(); }
}