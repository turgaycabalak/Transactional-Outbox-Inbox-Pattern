package com.outbox.analyticsservice.entity.inbox;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_inbox")
public class OrderInbox {
  @Id
  private UUID id; // outboxId, for idempotency

  @Column(nullable = false)
  private Long orderId;
  @Column(nullable = false)
  private Integer quantity;
  @Column(nullable = false)
  private String description;

  @Column(name = "occurred_on", nullable = false)
  private LocalDateTime occurredOn;

  @Column(name = "processed_on")
  private LocalDateTime processedOn;
}
