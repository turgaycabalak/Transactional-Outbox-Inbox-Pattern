//package com.outbox.orderservice.listener;
//
//import java.sql.Connection;
//import java.sql.Statement;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import jakarta.annotation.PostConstruct;
//
//import javax.sql.DataSource;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.PropertyNamingStrategies;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.outbox.orderservice.config.RabbitConfig;
//import com.outbox.orderservice.entity.outbox.OrderOutbox;
//import com.outbox.orderservice.event.OrderCreatedEvent;
//import com.outbox.orderservice.repository.OrderOutboxRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.postgresql.PGConnection;
//import org.postgresql.PGNotification;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Component;
//
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ReactiveOrderOutboxListener {
//  private final DataSource dataSource;
//  private final RabbitTemplate rabbitTemplate;
//  private final OrderOutboxRepository orderOutboxRepository;
//  private final ObjectMapper objectMapper = new ObjectMapper()
//      .registerModule(new JavaTimeModule())
//      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
//
//
//  @PostConstruct
//  public void start() {
//    Flux.<PGNotification>create(sink -> {
//          new Thread(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//              try (Connection conn = dataSource.getConnection()) {
//                PGConnection pgConn = conn.unwrap(PGConnection.class);
//                try (Statement stmt = conn.createStatement()) {
//                  stmt.execute("LISTEN order_outbox_created_notify");
//                }
//                log.info("Reactive LISTEN started");
//
//                while (true) {
//                  PGNotification[] notifications = pgConn.getNotifications(1000);
//                  if (notifications != null) {
//                    for (PGNotification notification : notifications) {
//                      sink.next(notification);
//                    }
//                  }
//                  try (Statement ping = conn.createStatement()) {
//                    ping.execute("SELECT 1");
//                  }
//                }
//              } catch (Exception e) {
//                log.error("LISTEN failed, retrying...", e);
//                try {
//                  Thread.sleep(5000);
//                } catch (InterruptedException ex) {
//                  Thread.currentThread().interrupt();
//                }
//              }
//            }
//          }).start();
//        })
//        .flatMap(notification -> Mono.fromRunnable(() -> handleNotification(notification))
//            .subscribeOn(Schedulers.boundedElastic()))
//        .onErrorContinue((err, obj) -> log.error("Error in stream", err))
//        .subscribe();
//  }
//
//  private void handleNotification(PGNotification notification) {
//    try {
//      OrderOutbox outbox = objectMapper.readValue(notification.getParameter(), OrderOutbox.class);
//      OrderCreatedEvent event = createOrderCreatedEvent(outbox);
//
//      rabbitTemplate.convertAndSend(
//          RabbitConfig.ORDER_EXCHANGE,
//          RabbitConfig.ORDER_CREATED_ROUTING_KEY,
//          event
//      );
//
//      orderOutboxRepository.updateProcessedDateBatch(List.of(outbox.getId()), LocalDateTime.now());
//
//      log.info("Processed outbox: {}", outbox.getId());
//    } catch (Exception e) {
//      log.error("Error processing notification", e);
//    }
//  }
//
//  private OrderCreatedEvent createOrderCreatedEvent(OrderOutbox outbox) {
//    JsonNode payload = outbox.getPayload();
//    return OrderCreatedEvent.builder()
//        .idempotentId(outbox.getId())
//        .orderId(payload.get("id").asLong())
//        .quantity(payload.get("quantity").asInt())
//        .description(payload.get("description").asText())
//        .build();
//  }
//}
