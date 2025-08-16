package com.outbox.analyticsservice.listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.outbox.analyticsservice.entity.OrderAnalyticsEntity;
import com.outbox.analyticsservice.entity.inbox.OrderInbox;
import com.outbox.analyticsservice.repository.OrderInboxRepository;
import com.outbox.analyticsservice.service.OrderInboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInboxListener {
  private final DataSource dataSource;
  private final OrderInboxRepository orderInboxRepository;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  private final OrderInboxService orderInboxService;


  @PostConstruct
  public void startListenerThread() {
    Thread thread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          listenWithRetry();

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.info("Listener thread interrupted, shutting down.");

        } catch (Exception e) {
          log.error("Unexpected error in listener loop", e);
        }
      }
    });
    thread.setDaemon(true);
    thread.start();
  }

  @Retryable(
      exceptionExpression = "#{#exception instanceof java.sql.SQLException}",
      maxAttempts = Integer.MAX_VALUE,
      backoff = @Backoff(delay = 5000)
  )
  public void listenWithRetry() throws InterruptedException, SQLException {
    try (Connection conn = dataSource.getConnection()) {
      PGConnection pgConn = conn.unwrap(PGConnection.class);

      try (Statement stmt = conn.createStatement()) {
        stmt.execute("LISTEN order_inbox_created_notify");
      }

      log.info("Listening for PostgreSQL NOTIFY on 'order_inbox_created_notify'");

      // Handling lost messages after connection is opened
      recoverMissedMessages();

      while (!Thread.currentThread().isInterrupted()) {
        PGNotification[] notifications = pgConn.getNotifications(1000);
        if (notifications != null) {
          Arrays.stream(notifications).forEach(this::handleNotification);
        }

        // Ping to keep connection alive
        try (Statement ping = conn.createStatement()) {
          ping.execute("SELECT 1");
        }
      }

    } catch (SQLException e) {
      log.error("PostgreSQL connection lost: {}", e.getMessage());
      throw e; // Triggers Spring Retry
    }
  }

  private void recoverMissedMessages() {
    List<OrderInbox> unprocessed = orderInboxRepository.findInboxesNotProcessed();
    List<OrderAnalyticsEntity> analytics = orderInboxService.createAndSaveAnalytics(unprocessed);

    List<UUID> orderInboxIds = unprocessed.stream()
        .map(OrderInbox::getId)
        .toList();
    log.info("Analytics processed for order inboxes: {}", orderInboxIds);
  }

  private void handleNotification(PGNotification notification) {
    try {
      int pid = notification.getPID();
      String name = notification.getName();
      String parameter = notification.getParameter();
      log.info("Notification PID: {} || name: {} || parameter: {}", pid, name, parameter);

      // todo: burda map edemiyor
      OrderInbox orderInbox = objectMapper.readValue(parameter, OrderInbox.class);


      List<OrderAnalyticsEntity> analytics = orderInboxService.createAndSaveAnalytics(List.of(orderInbox));
      List<Long> analyticsIds = analytics.stream()
          .map(OrderAnalyticsEntity::getId)
          .toList();

      log.info("Order analytics {} created for the Order Inbox: {}", analyticsIds, orderInbox.getId());
    } catch (JsonProcessingException e) {
      log.error("Error processing notification: {}", notification.getParameter(), e);
    }
  }
}
