package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.OrderEntity;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAcceptanceServiceTest {
    @Mock private OrderService orderService;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;
    private OrderAcceptanceService acceptanceService;

    @BeforeEach
    void setUp() {
        // Direct calls test worker logic; zero delay keeps unit tests fast.
        acceptanceService = new OrderAcceptanceService(
                orderService, kafkaTemplate, objectMapper, "test.order-events", 0);
    }

    @Test
    void acceptedOrderIsPublishedAfterDatabaseMethodReturns() {
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse accepted = new OrderResponse(42L, "team-1-trader1", "AAPL", Side.BUY,
                10L, new BigDecimal("185.50"), OrderStatus.ACCEPTED, now, now, 1L);
        when(orderService.acceptIfSubmitted(42L)).thenReturn(Optional.of(accepted));
        when(objectMapper.writeValueAsString(accepted)).thenReturn("accepted-json");
        when(kafkaTemplate.send("test.order-events", "42", "accepted-json"))
                .thenReturn(CompletableFuture.completedFuture(null));

        acceptanceService.acceptLater(42L);

        var sequence = inOrder(orderService, objectMapper, kafkaTemplate);
        sequence.verify(orderService).acceptIfSubmitted(42L);
        sequence.verify(objectMapper).writeValueAsString(accepted);
        sequence.verify(kafkaTemplate).send("test.order-events", "42", "accepted-json");
    }

    @Test
    void skippedOrderDoesNotPublish() {
        when(orderService.acceptIfSubmitted(42L)).thenReturn(Optional.empty());
        acceptanceService.acceptLater(42L);
        verifyNoInteractions(objectMapper, kafkaTemplate);
    }

    @Test
    void concurrentUpdateDoesNotPublishOrRetryStaleOrder() {
        when(orderService.acceptIfSubmitted(42L))
                .thenThrow(new ObjectOptimisticLockingFailureException(OrderEntity.class, 42L));
        assertDoesNotThrow(() -> acceptanceService.acceptLater(42L));
        verify(orderService).acceptIfSubmitted(42L);
        verifyNoInteractions(objectMapper, kafkaTemplate);
    }

    @Test
    void interruptionStopsProcessingAndPreservesInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            acceptanceService.acceptLater(42L);
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(orderService, objectMapper, kafkaTemplate);
        } finally {
            Thread.interrupted();
        }
    }
}
