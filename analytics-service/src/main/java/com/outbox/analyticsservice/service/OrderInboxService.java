package com.outbox.analyticsservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.outbox.analyticsservice.entity.AnalyzedForEnum;
import com.outbox.analyticsservice.entity.OrderAnalyticsEntity;
import com.outbox.analyticsservice.entity.inbox.OrderInbox;
import com.outbox.analyticsservice.repository.OrderInboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInboxService {
  private final OrderInboxRepository orderInboxRepository;
  private final OrderAnalyticsService orderAnalyticsService;


  public boolean existsById(UUID id) {
    return orderInboxRepository.existsById(id);
  }

  @Transactional
  public OrderInbox save(OrderInbox orderInboxToSave) {
    return orderInboxRepository.save(orderInboxToSave);
  }

  @Transactional
  public List<UUID> triggerOrderInboxes() {
    List<OrderInbox> inboxesNotProcessed = orderInboxRepository.findInboxesNotProcessed();
    log.info("Manual pulling publisher started processing. Found {} unprocessed Order Inboxes.",
        inboxesNotProcessed.size());

    List<OrderAnalyticsEntity> analytics = createAndSaveAnalytics(inboxesNotProcessed);
    return inboxesNotProcessed.stream()
        .map(OrderInbox::getId)
        .toList();
  }

  @Transactional
  public List<OrderAnalyticsEntity> createAndSaveAnalytics(List<OrderInbox> inboxesNotProcessed) {
    List<OrderAnalyticsEntity> orderAnalyticsToSaveAll = inboxesNotProcessed.stream()
        .flatMap(inbox -> Stream.of(
            createOrderAnalytics(inbox, AnalyzedForEnum.FOR_CUSTOMER),
            createOrderAnalytics(inbox, AnalyzedForEnum.FOR_COMPANY)
        ))
        .toList();
    List<OrderAnalyticsEntity> analyticsEntities = orderAnalyticsService.saveAll(orderAnalyticsToSaveAll);

    List<UUID> orderInboxIds = inboxesNotProcessed.stream()
        .map(OrderInbox::getId)
        .toList();
    orderInboxRepository.updateProcessedDateBatch(orderInboxIds, LocalDateTime.now());

    return analyticsEntities;
  }

  private OrderAnalyticsEntity createOrderAnalytics(OrderInbox inbox, AnalyzedForEnum analyzedForEnum) {
    double tax = analyzedForEnum.getTax();
    int quantity = inbox.getQuantity();
    double orderTotal = tax * quantity;

    String analyticsResults = String.format("Analyzed for %s: total order:%s", analyzedForEnum.name(), orderTotal);

    return OrderAnalyticsEntity.builder()
        .orderId(inbox.getOrderId())
        .tax(tax)
        .orderTotal(orderTotal)
        .itemCount(quantity)
        .analyzedFor(analyzedForEnum)
        .analyticsResults(analyticsResults)
        .build();
  }
}
