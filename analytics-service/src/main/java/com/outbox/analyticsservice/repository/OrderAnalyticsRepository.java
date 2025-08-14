package com.outbox.analyticsservice.repository;

import com.outbox.analyticsservice.entity.OrderAnalyticsEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAnalyticsRepository extends JpaRepository<OrderAnalyticsEntity, Long> {
}
