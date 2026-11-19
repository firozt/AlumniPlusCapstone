package com.alumniplus.order_service.dto;

import com.alumniplus.order_service.model.OrderEntity;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    Long orderId,
    String owner,
    String instrumentId,
    Side side,
    Long quantity,
    BigDecimal limitPrice,
    OrderStatus status,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public static OrderResponse from(OrderEntity o) {
    return new OrderResponse(
        o.getId(),
        o.getOwner(),
        o.getInstrument().getId(),
        o.getSide(),
        o.getQuantity(),
        o.getLimitPrice(),
        o.getStatus(),
        o.getCreatedAt(),
        o.getUpdatedAt(),
        o.getVersion());
  }
}
