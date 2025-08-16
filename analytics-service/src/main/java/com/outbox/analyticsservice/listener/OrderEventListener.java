package com.outbox.analyticsservice.listener;

import java.time.LocalDateTime;
import java.util.UUID;

import com.outbox.analyticsservice.config.RabbitConfig;
import com.outbox.analyticsservice.entity.inbox.OrderInbox;
import com.outbox.analyticsservice.event.OrderCreatedEvent;
import com.outbox.analyticsservice.service.OrderInboxService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class OrderEventListener {
  private final OrderInboxService orderInboxService;

  @Transactional
  @RabbitListener(queues = RabbitConfig.ORDER_ANALYTICS_QUEUE)
  public void handleCreatedOrderEvent(OrderCreatedEvent orderCreatedEvent) {
    UUID idempotentId = orderCreatedEvent.getIdempotentId();
    if (idempotentId == null) {
      return;
    }

    // Idempotency check
    boolean exists = orderInboxService.existsById(idempotentId);
    if (exists) {
      log.warn(
          "OrderCreatedEvent with idempotentId {} already exists in order_inbox with orderId: {}. Skipping processing.",
          idempotentId, orderCreatedEvent.getOrderId());
      return;
    }

    OrderInbox orderInboxToSave = createOrderInbox(orderCreatedEvent);
    OrderInbox savedOrderInbox = orderInboxService.save(orderInboxToSave);

    log.info("OrderCreatedEvent processed and saved to order_inbox. id/idempotentId: {}, orderId: {}",
        savedOrderInbox.getId(), savedOrderInbox.getOrderId());
  }

  private OrderInbox createOrderInbox(OrderCreatedEvent orderCreatedEvent) {
    return OrderInbox.builder()
        .id(orderCreatedEvent.getIdempotentId())
        .orderId(orderCreatedEvent.getOrderId())
        .quantity(orderCreatedEvent.getQuantity())
        .description(orderCreatedEvent.getDescription())
        .occurredOn(LocalDateTime.now())
        .processedOn(null)
        .build();
  }
}
