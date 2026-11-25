package com.alumniplus.order_service.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        String eventId,
        String eventType,
        Long orderId,
        String owner,
        String actor,
        Instant occurredAt) {

    public static OrderEvent of(OrderResponse o, String actor) {
        return new OrderEvent(
                UUID.randomUUID().toString(), // unique per event, so the Activity Service can ignore duplicates
                o.status().name(),            // SUBMITTED / ACCEPTED / CANCELLED
                o.orderId(),
                o.owner(),
                actor,
                o.updatedAt());
    }
}