package com.outbox.orderservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.outbox.orderservice.entity.outbox.OrderOutbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

  @Query("""
      SELECT box FROM OrderOutbox box
      WHERE box.processedOn IS NULL
      ORDER BY box.occurredOn DESC
      """)
  List<OrderOutbox> findOutboxesNotProcessed();

  @Transactional
  @Modifying
  @Query("UPDATE OrderOutbox o SET o.processedOn = :date WHERE o.id IN :ids")
  void updateProcessedDateBatch(@Param("ids") List<UUID> ids, @Param("date") LocalDateTime date);

}
