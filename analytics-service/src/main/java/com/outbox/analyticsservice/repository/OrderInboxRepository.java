package com.outbox.analyticsservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.outbox.analyticsservice.entity.inbox.OrderInbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderInboxRepository extends JpaRepository<OrderInbox, UUID> {

  @Query("""
      SELECT box FROM OrderInbox box
      WHERE box.processedOn IS NULL
      ORDER BY box.occurredOn DESC
      """)
  List<OrderInbox> findInboxesNotProcessed();

  @Transactional
  @Modifying
  @Query("UPDATE OrderInbox o SET o.processedOn = :date WHERE o.id IN :ids")
  void updateProcessedDateBatch(@Param("ids") List<UUID> ids, @Param("date") LocalDateTime date);

}
