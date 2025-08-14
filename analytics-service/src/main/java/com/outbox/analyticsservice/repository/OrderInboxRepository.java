package com.outbox.analyticsservice.repository;

import java.util.List;
import java.util.UUID;

import com.outbox.analyticsservice.entity.inbox.OrderInbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderInboxRepository extends JpaRepository<OrderInbox, UUID> {

  @Query("""
      SELECT box FROM OrderInbox box
      WHERE box.processedDate IS NULL
      ORDER BY box.occurredOn DESC
      """)
  List<OrderInbox> findInboxesNotProcessed();
}
