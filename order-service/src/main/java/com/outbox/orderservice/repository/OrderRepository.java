package com.outbox.orderservice.repository;

import com.outbox.orderservice.entity.OrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
