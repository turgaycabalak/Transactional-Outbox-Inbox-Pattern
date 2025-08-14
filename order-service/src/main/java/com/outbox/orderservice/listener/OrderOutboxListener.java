package com.outbox.orderservice.listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.outbox.orderservice.config.RabbitConfig;
import com.outbox.orderservice.entity.outbox.OrderOutbox;
import com.outbox.orderservice.event.OrderCreatedEvent;
import com.outbox.orderservice.repository.OrderOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxListener {
  private final DataSource dataSource;
  private final RabbitTemplate rabbitTemplate;
  private final OrderOutboxRepository orderOutboxRepository;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);


  @PostConstruct
  public void startListenerThread() {
    Thread thread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          listenWithRetry();
        } catch (InterruptedException | SQLException e) {
          Thread.currentThread().interrupt();
          log.info("Listener thread interrupted, shutting down.");
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
        stmt.execute("LISTEN order_outbox_created_notify");
      }

      log.info("Listening for PostgreSQL NOTIFY on 'order_outbox_created_notify'");

      // Bağlantı açıldıktan sonra kayıp mesajları işleme
      recoverMissedMessages();

      while (!Thread.currentThread().isInterrupted()) {
        PGNotification[] notifications = pgConn.getNotifications(1000);
        if (notifications != null) {
          for (PGNotification notification : notifications) {
            handleNotification(notification);
          }
        }

        // Connection canlı kalması için ping
        try (Statement ping = conn.createStatement()) {
          ping.execute("SELECT 1");
        }
      }

    } catch (SQLException e) {
      log.error("PostgreSQL connection lost: {}", e.getMessage());
      throw e; // Spring Retry yeniden bağlanmayı tetikler
    }
  }

  private void recoverMissedMessages() {
    List<OrderOutbox> unprocessed = orderOutboxRepository.findOutboxesNotProcessed();
    for (OrderOutbox orderOutbox : unprocessed) {
      try {
        OrderCreatedEvent event = createOrderCreatedEvent(orderOutbox);
        rabbitTemplate.convertAndSend(
            RabbitConfig.ORDER_EXCHANGE,
            RabbitConfig.ORDER_CREATED_ROUTING_KEY,
            event
        );
        orderOutboxRepository.updateProcessedDateBatch(List.of(orderOutbox.getId()), LocalDateTime.now());
        log.info("Recovered and processed outbox: {}", orderOutbox.getId());
      } catch (Exception e) {
        log.error("Error recovering outbox {}", orderOutbox.getId(), e);
      }
    }
  }

  private void handleNotification(PGNotification notification) {
    try {
      OrderOutbox orderOutbox = objectMapper.readValue(notification.getParameter(), OrderOutbox.class);
      OrderCreatedEvent event = createOrderCreatedEvent(orderOutbox);

      rabbitTemplate.convertAndSend(
          RabbitConfig.ORDER_EXCHANGE,
          RabbitConfig.ORDER_CREATED_ROUTING_KEY,
          event
      );

      orderOutboxRepository.updateProcessedDateBatch(
          List.of(orderOutbox.getId()), LocalDateTime.now()
      );

      log.info("Processed outbox: {}", orderOutbox.getId());

    } catch (Exception e) {
      log.error("Error processing notification: {}", notification.getParameter(), e);
    }
  }

  private OrderCreatedEvent createOrderCreatedEvent(OrderOutbox outbox) {
    JsonNode payload = outbox.getPayload();
    return OrderCreatedEvent.builder()
        .idempotentId(outbox.getId())
        .orderId(payload.get("id").asLong())
        .quantity(payload.get("quantity").asInt())
        .description(payload.get("description").asText())
        .build();
  }
}
