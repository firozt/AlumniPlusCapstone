package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${app.kafka.orders-submitted-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    // Call after the database transaction has committed.
    public void publish(OrderResponse order) {
        try {
            String payload = objectMapper.writeValueAsString(order);
            kafkaTemplate.send(topic, order.orderId().toString(), payload).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publishFailure(order, exception);
        } catch (Exception exception) {
            throw publishFailure(order, exception);
        }
    }

    private ResponseStatusException publishFailure(OrderResponse order, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Order " + order.orderId() + " was saved as " + order.status()
                        + " but Kafka publishing was not confirmed", cause);
    }
}
