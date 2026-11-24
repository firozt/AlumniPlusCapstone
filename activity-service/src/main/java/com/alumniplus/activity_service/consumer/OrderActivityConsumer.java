
package com.alumniplus.activity_service.consumer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.alumniplus.activity_service.dto.OrderActivityEvent;
import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.service.OrderActivityService;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderActivityConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderActivityConsumer.class);

    private final OrderActivityService service;
    private final ObjectMapper objectMapper;

    public OrderActivityConsumer(
            OrderActivityService service,
            ObjectMapper objectMapper) {

        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.order-topic}")
    public void consume(String message) {

        OrderActivityEvent event = objectMapper.readValue(
                message,
                OrderActivityEvent.class
        );

        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                event.eventTime(),
                ZoneOffset.UTC
        );

        OrderActivity activity = new OrderActivity(
                event.eventId(),
                event.orderId(),
                event.eventType(),
                occurredAt,
                event.actor()
        );

        activity.setReceivedAt(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        service.recordActivity(activity);

        logger.info(
                "Processed activity: eventId={}, orderId={}, type={}",
                event.eventId(),
                event.orderId(),
                event.eventType()
        );
    }
}
