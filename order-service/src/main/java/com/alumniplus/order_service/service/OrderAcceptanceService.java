package com.alumniplus.order_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Service
public class OrderAcceptanceService {

    private static final Logger log = LoggerFactory.getLogger(OrderAcceptanceService.class);
    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final long delayMs;

    public OrderAcceptanceService(OrderService orderService, KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, @Value("${app.kafka.orders-submitted-topic}") String topic,
            @Value("${app.orders.acceptance-delay-ms}") long delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("Acceptance delay must not be negative");
        }
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.delayMs = delayMs;
    }

    // spawns another thread sleeps for N seconds (application.properties) then 
    // pulls to see any updates if not sets to accepted
    @Async("orderAcceptanceExecutor")
    public void acceptLater(Long orderId) {
        try {
            // wait
            Thread.sleep(delayMs);
            var accepted = orderService.acceptIfSubmitted(orderId);
            if (accepted.isEmpty()) {
                return;
            }

            // send this update to kafka
            String payload = objectMapper.writeValueAsString(accepted.get());
            kafkaTemplate.send(topic, orderId.toString(), payload).get(30, TimeUnit.SECONDS);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.info("Acceptance interrupted for orderId={}", orderId);

        } catch (ObjectOptimisticLockingFailureException exception) {
            log.info("Acceptance skipped after concurrent update for orderId={}", orderId);

        } catch (Exception exception) {
            log.error("Acceptance or Kafka publishing failed for orderId={}", orderId, exception);

        }
    }
}
