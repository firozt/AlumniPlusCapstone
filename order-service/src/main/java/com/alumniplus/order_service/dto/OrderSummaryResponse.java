package com.alumniplus.order_service.dto;

import java.math.BigDecimal;

public record OrderSummaryResponse(
        long orderCount,
        BigDecimal totalRequestedValue
) {}
