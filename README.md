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
