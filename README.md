# Order-Analytics Event Driven Demo  
### Transactional Outbox + Inbox Pattern with PostgreSQL NOTIFY and RabbitMQ

This project demonstrates a resilient event-driven microservice architecture using **Transactional Outbox + Inbox Pattern**.  
It guarantees **at-least-once delivery**, **idempotent processing**, and **eventual consistency** across services.

---

## ✅ Goal

When an order is created in the **Order Service**, an event is reliably propagated to the **Analytics Service**, which then creates two analytics entries related to that order.

The delivery is fault-tolerant:
- If RabbitMQ is down → event will be retried later
- If consumer fails → inbox record protects against message loss
- Duplicate events are safely ignored (by UUID)

---

## 🧩 Main Concepts

| Pattern / Mechanism             | Usage in this Project |
|----------------------------------|------------------------|
| Transactional Outbox             | Write order + outbox event in same DB transaction |
| PostgreSQL LISTEN / NOTIFY       | Almost real-time trigger after insert |
| RabbitMQ                         | Message broker for inter-service events |
| Inbox Pattern                    | Consumer-side durability + idempotency |
| Idempotent Insert (UUID PK)      | Prevent duplicate event processing |
| Eventual Consistency             | Data converges eventually between services |

---

## 📁 Modules

### 1. order-service
- Writes order + matching row in `order_outbox` (single transaction)
- PostgreSQL trigger fires a NOTIFY (`order_outbox_created_notify`)
- Application listens to NOTIFY, publishes to RabbitMQ
- On startup: re-sends all `processed_on IS NULL` outbox events first

### 2. analytics-service
- Listens same RabbitMQ queue
- Inserts message into `order_inbox` (UUID → idempotent)
- DB trigger fires a NOTIFY (`order_inbox_created_notify`)
- Application listens and creates **two rows** in `order_analytics` table
- On startup: processes pending inbox rows first (`processed_on IS NULL`)

---

## 🔁 Event Flow Diagram

```mermaid
sequenceDiagram
    participant O as Order Service
    participant DB1 as order_outbox (Postgres)
    participant MQ as RabbitMQ
    participant DB2 as order_inbox (Analytics DB)
    participant A as Analytics Service

    O->>DB1: INSERT order + outbox (same transaction)
    DB1-->>O: NOTIFY order_outbox_created_notify
    O->>MQ: Publish OrderCreatedEvent
    MQ->>A: Event consumed
    A->>DB2: INSERT into order_inbox (idempotent)
    DB2-->>A: NOTIFY order_inbox_created_notify
    A->>A: Insert 2 analytics records
````

## 🔁 Event Flow Diagram Detailed

```mermaid
sequenceDiagram
    participant O as Order Service
    participant DB1 as order_outbox (Postgres)
    participant MQ as RabbitMQ
    participant A as Analytics Service
    participant DB2 as order_inbox (Analytics DB)
    participant Scheduler as Order Outbox Scheduler

    Note over O,DB1: Step 1: Create Order
    O->>DB1: INSERT order + outbox row (same transaction)
    DB1-->>O: Commit OK

    Note over DB1,O: Notify mechanism triggers
    DB1-->>O: NOTIFY order_outbox_created_notify

    Note over O,MQ: Step 2: Publish Event
    O->>MQ: Publish OrderCreatedEvent
    alt MQ unavailable
        Note over Scheduler: Scheduler retries unprocessed outbox rows
        Scheduler->>MQ: Retry publish OrderCreatedEvent
    end

    Note over MQ,A: Step 3: Consume Event
    MQ->>A: Event consumed
    A->>DB2: INSERT into order_inbox (idempotent)
    alt Duplicate ID
        DB2--xA: Ignore
    else New ID
        DB2-->>A: Success
        DB2-->>A: NOTIFY order_inbox_created_notify
    end

    Note over A: Step 4: Analytics Processing
    A->>A: Insert 2 analytics records
    A->>DB2: UPDATE order_inbox processed_date
