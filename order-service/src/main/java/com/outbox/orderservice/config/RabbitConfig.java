package com.outbox.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {
  public static final String ORDER_EXCHANGE = "orderExchange";
  public static final String ORDER_ANALYTICS_QUEUE = "orderAnalyticsQueue";
  public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter());
    template.setMandatory(true);
    return template;
  }

  @Bean
  public DirectExchange orderExchange() {
    return new DirectExchange(ORDER_EXCHANGE);
  }

  @Bean
  public Queue orderAnalyticsQueue() {
    return QueueBuilder
        .durable(ORDER_ANALYTICS_QUEUE)
        .build();
  }

  @Bean
  public Binding paymentOrderBinding(Queue orderAnalyticsQueue, DirectExchange orderExchange) {
    return BindingBuilder
        .bind(orderAnalyticsQueue)
        .to(orderExchange)
        .with(ORDER_CREATED_ROUTING_KEY);
  }
}
