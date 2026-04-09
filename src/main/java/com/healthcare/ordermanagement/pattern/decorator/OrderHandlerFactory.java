package com.healthcare.ordermanagement.pattern.decorator;

import com.healthcare.ordermanagement.pattern.command.CommandLog;
import com.healthcare.ordermanagement.resource.InMemoryOrderAccess;
import com.healthcare.ordermanagement.resource.OrderAccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * CHANGE 2b: factory now injects Clock, OrderAccess and CommandLog so the
 * two new decorators (PriorityEscalationDecorator, StatAuditDecorator) can
 * be wired into the chain without touching any other pre-existing file.
 *
 * Chain (outermost → innermost, runs top-to-bottom):
 *   ValidationDecorator
 *     → TimedPriorityBoostingDecorator  (long-wait URGENT → STAT)
 *       → PriorityEscalationDecorator   (URGENT near recent STAT → STAT)
 *         → StatAuditDecorator          (extra audit entry for STAT orders)
 *           → AuditLoggingDecorator
 *             → BaseOrderHandler
 */
@Component
public class OrderHandlerFactory {

    private final BaseOrderHandler baseOrderHandler;
    private final OrderAccess      orderAccess;
    private final Clock            clock;
    private final CommandLog       commandLog;

    // Spring constructor (all dependencies injected by Spring)
    @Autowired
    public OrderHandlerFactory(BaseOrderHandler baseOrderHandler,
                               OrderAccess orderAccess,
                               Clock clock,
                               CommandLog commandLog) {
        this.baseOrderHandler = baseOrderHandler;
        this.orderAccess      = orderAccess;
        this.clock            = clock;
        this.commandLog       = commandLog;
    }

    // Backward-compatible constructor for existing unit tests (no Spring context)
    public OrderHandlerFactory(BaseOrderHandler baseOrderHandler) {
        this(baseOrderHandler,
             new InMemoryOrderAccess(),
             Clock.systemDefaultZone(),
             new CommandLog());
    }

    public OrderHandler buildChain() {
        OrderHandler handler = baseOrderHandler;
        handler = new AuditLoggingDecorator(handler);
        handler = new StatAuditDecorator(handler, orderAccess, commandLog);
        handler = new PriorityEscalationDecorator(handler, orderAccess, clock);
        handler = new TimedPriorityBoostingDecorator(handler, clock);
        handler = new ValidationDecorator(handler);
        return handler;
    }
}