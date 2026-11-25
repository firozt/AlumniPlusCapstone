package com.alumniplus.activity_service.consumer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

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

    private static final Logger log =
        LoggerFactory.getLogger(OrderActivityConsumer.class);

    private static final Set<String> VALID_STATUSES =
        Set.of("SUBMITTED", "ACCEPTED", "CANCELLED");

    private final ObjectMapper objectMapper;
    private final OrderActivityService activityService;

    public OrderActivityConsumer(
            ObjectMapper objectMapper,
            OrderActivityService activityService) {

        this.objectMapper = objectMapper;
        this.activityService = activityService;
    }

    @KafkaListener(topics = "${app.kafka.order-topic}")
    public void consume(String message) {

        OrderActivityEvent event =
            objectMapper.readValue(message, OrderActivityEvent.class);

        validateEvent(event);

        String eventId = generateEventId(
            event.orderId(),
            event.version()
        );

        LocalDateTime occurredAt = LocalDateTime.ofInstant(
            event.updatedAt(),
            ZoneOffset.UTC
        );

        OrderActivity activity = new OrderActivity(
            eventId,
            event.orderId(),
            event.status(),
            occurredAt,
            event.owner()
        );

        activity.setReceivedAt(
            LocalDateTime.now(ZoneOffset.UTC)
        );

        activityService.recordActivity(activity);

        log.info(
            "Processed order activity: eventId={}, orderId={}, eventType={}",
            eventId,
            event.orderId(),
            event.status()
        );
    }

    private String generateEventId(Long orderId, Long version) {

        String identifier = orderId + ":" + version;

        return UUID.nameUUIDFromBytes(
            identifier.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private void validateEvent(OrderActivityEvent event) {

        if (event == null
                || event.orderId() == null
                || event.orderId() <= 0
                || event.version() == null
                || event.version() < 0
                || event.owner() == null
                || event.owner().isBlank()
                || event.owner().length() > 100
                || event.updatedAt() == null
                || event.status() == null
                || !VALID_STATUSES.contains(event.status())) {

            throw new IllegalArgumentException(
                "Invalid order activity event"
            );
        }
    }
}