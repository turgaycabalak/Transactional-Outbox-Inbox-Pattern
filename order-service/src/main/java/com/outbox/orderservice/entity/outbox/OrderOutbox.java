package com.outbox.orderservice.entity.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.outbox.orderservice.entity.OrderEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_outbox")
public class OrderOutbox {
  @Id
  private UUID id; // will be used for idempotency...

  //@OneToOne(fetch = FetchType.LAZY)
  //@JoinColumn(name = "order_id", nullable = false)
  //private OrderEntity order;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private String payload; // json of the related OrderEntity

  @Column(nullable = false)
  private String eventType;

  @Column(name = "occurred_on", nullable = false)
  private LocalDateTime occurredOn;

  @Column(name = "processed_on")
  private LocalDateTime processedDate;
}
