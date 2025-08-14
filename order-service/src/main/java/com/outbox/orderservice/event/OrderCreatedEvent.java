package com.outbox.orderservice.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
  private UUID idempotentId; // outboxId, for idempotency
  private Long orderId;
  private int quantity;
  private String description;
}
