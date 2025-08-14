package com.outbox.analyticsservice.controller;

import java.util.List;
import java.util.UUID;

import com.outbox.analyticsservice.service.OrderInboxService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-analytics")
@RequiredArgsConstructor
public class OrderAnalyticsController {

  private final OrderInboxService orderInboxService;

  // Manual pulling publisher
  @GetMapping("/trigger")
  public List<UUID> triggerOrderInboxes() {
    return orderInboxService.triggerOrderInboxes();
  }
}
