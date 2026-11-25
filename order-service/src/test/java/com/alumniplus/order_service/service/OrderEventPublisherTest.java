package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private CompletableFuture<SendResult<String, String>> future;
    private OrderEventPublisher publisher;
    private OrderResponse order;

    @BeforeEach
    void setUp() {
        publisher = new OrderEventPublisher(kafkaTemplate, objectMapper, "test.order-events");
        order = sampleOrder(OrderStatus.CANCELLED);
    }

    //happy path
    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void publishesEveryStatusWithOrderIdKeyAndWaitsForAcknowledgement(OrderStatus status) throws Exception {
        // given -> an order status, serialized JSON, and a successful Kafka future
        order = sampleOrder(status);
        when(objectMapper.writeValueAsString(order)).thenReturn("order-json");
        when(kafkaTemplate.send("test.order-events", "42", "order-json")).thenReturn(future);
        when(future.get(30, TimeUnit.SECONDS)).thenReturn(null);

        // when -> publish is called
        publisher.publish(order);

        // then -> serialize and send to the configured topic with the order ID key, then await acknowledgement
        var sequence = inOrder(objectMapper, kafkaTemplate, future);
        sequence.verify(objectMapper).writeValueAsString(order);
        sequence.verify(kafkaTemplate).send("test.order-events", "42", "order-json");
        sequence.verify(future).get(30, TimeUnit.SECONDS);
    }

    @Test
    void failedAcknowledgementReturnsUnavailable() {
        // given -> a serialized order and a failed Kafka future
        when(objectMapper.writeValueAsString(order)).thenReturn("order-json");
        when(kafkaTemplate.send("test.order-events", "42", "order-json"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka down")));
        // when -> publish is attempted
        // then -> return 503 with the saved order status in the reason
        assertFailure();
    }

    @Test
    void immediateSendFailureReturnsUnavailable() {
        // given -> a serialized order and a producer that throws on send
        when(objectMapper.writeValueAsString(order)).thenReturn("order-json");
        when(kafkaTemplate.send("test.order-events", "42", "order-json"))
                .thenThrow(new IllegalStateException("Producer unavailable"));
        // when -> publish is attempted
        // then -> return 503
        assertFailure();
    }

    // failed to turn object to string within send function
    @Test
    void serializationFailureDoesNotSend() {
        // given -> a serializer that throws
        when(objectMapper.writeValueAsString(order)).thenThrow(new IllegalStateException("Invalid payload"));
        // when -> publish is attempted
        // then -> return 503 without sending anything to Kafka
        assertFailure();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void timeoutReturnsUnavailableWithoutWaitingInTest() throws Exception {
        // given -> a Kafka future whose timed get throws TimeoutException
        stubSend();
        when(future.get(30, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        // when -> publish waits for acknowledgement
        // then -> return 503 without actually waiting in the test
        assertFailure();
    }

    // HELPERS

    private void stubSend() {
        when(objectMapper.writeValueAsString(order)).thenReturn("order-json");
        when(kafkaTemplate.send("test.order-events", "42", "order-json")).thenReturn(future);
    }

    private void assertFailure() {
        var thrown = assertThrows(ResponseStatusException.class, () -> publisher.publish(order));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatusCode());
        assertEquals("Order 42 was saved as CANCELLED but Kafka publishing was not confirmed", thrown.getReason());
    }

    private OrderResponse sampleOrder(OrderStatus status) {
        Instant now = Instant.parse("2026-11-25T12:00:00Z");
        return new OrderResponse(42L, "team-1-trader1", "AAPL", Side.BUY, 10L,
                new BigDecimal("185.50"), status, now, now, 1L);
    }
}

