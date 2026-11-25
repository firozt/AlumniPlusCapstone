package com.alumniplus.order_service.dto;

// Internal result used to avoid publishing repeated cancellations.
public record CancellationResult(OrderResponse order, boolean changed) {}
