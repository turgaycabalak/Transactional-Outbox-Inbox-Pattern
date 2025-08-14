package com.outbox.orderservice.service;

import java.time.LocalDateTime;

import com.outbox.orderservice.dto.OrderCreateRequest;
import com.outbox.orderservice.entity.OrderEntity;
import com.outbox.orderservice.entity.outbox.OrderOutbox;
import com.outbox.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final OrderOutboxService orderOutboxService;

  @Transactional
  public OrderEntity create(OrderCreateRequest orderCreateRequest) {
    OrderEntity savedOrderEntity = saveOrder(orderCreateRequest.getQuantity(), orderCreateRequest.getDescription());
    log.info("Order created: {}", savedOrderEntity.getId());

    OrderOutbox savedOrderOutbox = orderOutboxService.saveOrderOutbox(savedOrderEntity, "OrderCreatedEvent");
    log.info("Order Outbox created: {}", savedOrderOutbox.getId());

    return savedOrderEntity;
  }

  private OrderEntity saveOrder(int quantity, String description) {
    OrderEntity orderToSave = OrderEntity.builder()
        .createdAt(LocalDateTime.now())
        .quantity(quantity)
        .description(description)
        .build();
    return orderRepository.save(orderToSave);
  }
}
