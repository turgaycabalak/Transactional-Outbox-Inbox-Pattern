package com.outbox.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbox.orderservice.config.RabbitConfig;
import com.outbox.orderservice.entity.OrderEntity;
import com.outbox.orderservice.entity.outbox.OrderOutbox;
import com.outbox.orderservice.event.OrderCreatedEvent;
import com.outbox.orderservice.repository.OrderOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOutboxService {
  private final OrderOutboxRepository orderOutboxRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @Transactional
  public OrderOutbox saveOrderOutbox(OrderEntity orderEntity, String eventType) {
    try {
      String payload = objectMapper.writeValueAsString(orderEntity);

      OrderOutbox toSaveOrderOutbox = createOrderOutbox(eventType, payload);

      return orderOutboxRepository.save(toSaveOrderOutbox);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private OrderOutbox createOrderOutbox(String eventType, String payload) {
    return OrderOutbox.builder()
        .id(UUID.randomUUID())
        .payload(payload)
        .eventType(eventType)
        .occurredOn(LocalDateTime.now())
        .processedDate(null) // will be set after sending to eventbus
        .build();
  }

  public List<UUID> triggerOrderOutboxes() {
    List<OrderOutbox> outboxesNotProcessed = orderOutboxRepository.findOutboxesNotProcessed();
    log.info("Manual pulling publisher started processing. Found {} unprocessed Order Outboxes.",
        outboxesNotProcessed.size());

    List<UUID> processedIds = outboxesNotProcessed.parallelStream()
        .map(outbox -> {
          try {
            OrderCreatedEvent orderCreatedEvent = createOrderCreatedEvent(outbox);

            rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_CREATED_ROUTING_KEY,
                orderCreatedEvent
            );

            return outbox.getId(); // return the successful outbox id
          } catch (Exception e) {
            log.error("OrderOutbox publish failed. ID: {}", outbox.getId(), e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toList();

    if (!processedIds.isEmpty()) {
      orderOutboxRepository.updateProcessedDateBatch(processedIds, LocalDateTime.now());
    }

    return processedIds;
  }

  private OrderCreatedEvent createOrderCreatedEvent(OrderOutbox outbox) {
    try {
      // instead of using deserializing, you can access OrderEntity from the relational object(one-to-one) if you want
      OrderEntity orderFromJson = objectMapper.readValue(outbox.getPayload(), OrderEntity.class);

      return OrderCreatedEvent.builder()
          .orderId(orderFromJson.getId())
          .idempotentId(outbox.getId())
          .quantity(orderFromJson.getQuantity())
          .description(orderFromJson.getDescription())
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
