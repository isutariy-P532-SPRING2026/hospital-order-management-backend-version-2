# Hospital Order Management — Backend

## Live Deployment

Backend API: <https://hospital-order-backend-p2.onrender.com/>
Frontend: [https://isutariy-p532-spring2026.github.io/hospital-order-management-frontend-version-2/](https://isutariy-p532-spring2026.github.io/hospital-order-management-frontend-version-2/)

## Tech Stack

- Java 21
- Spring Boot 3.5.1
- Maven
- Docker
- JUnit 5 + Mockito

## Architecture — The Method (4-Layer)

```
Client (REST Controllers)
    └── Manager (OrderManager)
            ├── Engine (TriagingEngine)
            ├── Resource Access (OrderAccess)
            └── Utility (NotificationService)
```

## Design Patterns

| Pattern   | Class(es)                                                       | Layer     |
|-----------|-----------------------------------------------------------------|-----------|
| Strategy  | TriageStrategy, PriorityTriageStrategy, LoadBalancingTriageStrategy, DeadlineFirstTriageStrategy, TriageStrategyHolder | Engine |
| Observer  | NotificationService, CompositeNotificationService, ConsoleNotificationService, InAppNotificationService, EmailNotificationService | Utility |
| Decorator | OrderHandler, ValidationDecorator, TimedPriorityBoostingDecorator, PriorityEscalationDecorator, StatAuditDecorator, AuditLoggingDecorator | Manager |
| Factory   | OrderFactory                                                    | Manager   |
| Command   | Command, SubmitOrderCommand, ClaimOrderCommand, CompleteOrderCommand, CancelOrderCommand, CommandLog | Manager |

## Week 2 Changes

| Change | Pre-existing files modified | New files |
|---|---|---|
| Change 1 — Department-Aware Triage | 0 | TriageStrategyHolder (@Primary), LoadBalancingTriageStrategy, DeadlineFirstTriageStrategy, TriageStrategyController |
| Change 2a — Multi-Channel Notifications | 1 (ConsoleNotificationService — structured output) | CompositeNotificationService, InAppNotificationService, EmailNotificationService, NotificationConfig, NotificationPreferences, NotificationMessage, NotificationPreferenceController |
| Change 2b — Order Processing Decorators | 1 (OrderHandlerFactory — new chain) | TimedPriorityBoostingDecorator, PriorityEscalationDecorator, StatAuditDecorator, ClockConfig |
| Change 3 — Command Undo & Replay | 2 (CommandLog — replayAt/recordNote; OrderManager — queue sync) | ReplayController |

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/orders | Submit a new order |
| GET | /api/orders/queue | Get sorted triage queue |
| GET | /api/orders | Get all orders |
| PATCH | /api/orders/{id}/cancel | Cancel a pending order |
| POST | /api/fulfilment/claim | Claim next order |
| PATCH | /api/fulfilment/{id}/complete | Complete a claimed order |
| GET | /api/audit | Get audit trail |
| POST | /api/audit/undo | Undo last command |
| POST | /api/audit/replay/{index} | Replay command at index |
| GET | /api/triage/strategy | Get active triage strategy |
| POST | /api/triage/strategy | Set triage strategy at runtime |
| GET | /api/notifications/preferences | Get channel preferences |
| POST | /api/notifications/preferences | Update channel preferences |
| GET | /api/notifications/badge | Get unread notification count |
| GET | /api/notifications/messages | Get all in-app messages |
| POST | /api/notifications/messages/read | Mark all messages read |
| DELETE | /api/notifications/messages/{id} | Delete one message |
| DELETE | /api/notifications/messages | Clear all messages |

## Run Locally

```bash
# Maven
mvn spring-boot:run

# Docker
docker build -t ordersystem-p2 .
docker run -p 8080:8080 ordersystem-p2
```

Visit: <http://localhost:8080/api/orders/queue>

## Run Tests

```bash
mvn test

```

## Design Document

The full design document with diagrams is in **DesignDocument.pdf** in this repository.

Open it in a browser to view:

- Layered component diagram (all 6 layers)
- Call chain — Submit Order (all 5 patterns firing)
- Call chain — Fulfil Order (Claim + Complete)
- Design pattern justifications table
  