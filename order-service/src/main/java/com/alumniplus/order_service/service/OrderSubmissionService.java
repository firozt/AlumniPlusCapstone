package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderEvent;
import com.alumniplus.order_service.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;

@Service
public class OrderSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(OrderSubmissionService.class);

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final OrderAcceptanceService acceptanceService;

    public OrderSubmissionService(OrderService orderService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, @Value("${app.kafka.orders-submitted-topic}") String topic,
            OrderAcceptanceService acceptanceService) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.acceptanceService = acceptanceService;
    }

    // creates order using order service then pushes to kafka
    public OrderResponse submitOrder(CreateOrderRequest request, String owner) {
        OrderResponse order = orderService.createOrder(request, owner);

        try {
            String payload = objectMapper.writeValueAsString(OrderEvent.of(order, owner));
            kafkaTemplate.send(topic, order.orderId().toString(), payload) .get(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publishFailure(order, exception);
        } catch (Exception exception) {
            throw publishFailure(order, exception);
        }
        try {
            acceptanceService.acceptLater(order.orderId());
        } catch (TaskRejectedException exception) {
            log.error("Acceptance queue full; orderId={} remains SUBMITTED", order.orderId(), exception);
        }
        return order;
    }

    public OrderResponse cancelOrder(Long orderId, String owner) {
    OrderResponse order = orderService.cancelOrder(orderId, owner);
    try {
        String payload = objectMapper.writeValueAsString(OrderEvent.of(order, owner));
        kafkaTemplate.send(topic, orderId.toString(), payload).get(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw publishFailure(order, e);
    } catch (Exception e) {
        throw publishFailure(order, e);
    }
    return order;
}

    private ResponseStatusException publishFailure(OrderResponse order, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Order " + order.orderId() + " was saved but Kafka publishing was not confirmed", cause);
    }
}
