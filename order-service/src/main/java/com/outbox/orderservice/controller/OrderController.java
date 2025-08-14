package com.outbox.orderservice.controller;

import java.util.List;
import java.util.UUID;

import com.outbox.orderservice.dto.OrderCreateRequest;
import com.outbox.orderservice.entity.OrderEntity;
import com.outbox.orderservice.service.OrderOutboxService;
import com.outbox.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;
  private final OrderOutboxService orderOutboxService;

  @PostMapping
  public String create(@RequestBody OrderCreateRequest orderCreateRequest) {
    OrderEntity orderEntity = orderService.create(orderCreateRequest);
    return "Order created: " + orderEntity.getId();
  }

  // Manual pulling publisher
  @GetMapping("/trigger")
  public List<UUID> triggerOrderOutboxes() {
    return orderOutboxService.triggerOrderOutboxes();
  }
}
