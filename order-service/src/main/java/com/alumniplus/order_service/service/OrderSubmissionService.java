package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Service
public class OrderSubmissionService {

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderSubmissionService(OrderService orderService,
            KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${app.kafka.orders-submitted-topic}") String topic) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public OrderResponse submitOrder(CreateOrderRequest request, String owner) {
        // The separate service commits the order before Kafka consumers can receive it.
        OrderResponse order = orderService.createOrder(request, owner);

        try {
            String payload = objectMapper.writeValueAsString(order);
            kafkaTemplate.send(topic, order.orderId().toString(), payload)
                    .get(30, TimeUnit.SECONDS);
            return order;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publishFailure(order, exception);
        } catch (Exception exception) {
            throw publishFailure(order, exception);
        }
    }

    private ResponseStatusException publishFailure(OrderResponse order, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Order " + order.orderId() + " was saved but Kafka publishing was not confirmed", cause);
    }
}
