package com.outbox.analyticsservice.service;

import java.util.List;

import com.outbox.analyticsservice.entity.OrderAnalyticsEntity;
import com.outbox.analyticsservice.repository.OrderAnalyticsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAnalyticsService {
  private final OrderAnalyticsRepository orderAnalyticsRepository;

  @Transactional
  public List<OrderAnalyticsEntity> saveAll(List<OrderAnalyticsEntity> orderAnalyticsToSaveAll) {
    return orderAnalyticsRepository.saveAll(orderAnalyticsToSaveAll);
  }
}
