package com.alumniplus.order_service.dto;

import com.alumniplus.order_service.model.Side;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotNull(message = "instrumentId is required")
        Long instrumentId,

        @NotNull(message = "side must be BUY or SELL")
        Side side,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0")
        @Max(value = 999_999_999_999L, message = "quantity is too large")
        Long quantity,

        @NotNull(message = "limitPrice is required")
        @Positive(message = "limitPrice must be greater than 0")
        @Digits(integer = 8, fraction = 4, message = "limitPrice allows up to 8 whole digits and 4 decimals")
        BigDecimal limitPrice
) {}
