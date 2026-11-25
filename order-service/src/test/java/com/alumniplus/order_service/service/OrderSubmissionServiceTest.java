package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSubmissionServiceTest {

    private static final String TOPIC = "test.order-events";
    private static final String OWNER = "team-1-trader1";
    private static final String PAYLOAD = "{\"orderId\":42,\"status\":\"SUBMITTED\"}";

    @Mock
    private OrderService orderService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderAcceptanceService acceptanceService;

    @Mock
    private CompletableFuture<SendResult<String, String>> sendFuture;

    @Mock
    private SendResult<String, String> sendResult;

    private OrderSubmissionService submissionService;
    private CreateOrderRequest request;
    private OrderResponse savedOrder;

    // called on before each test, sets up data to be used
    @BeforeEach
    void setUp() {
        submissionService = new OrderSubmissionService(orderService, kafkaTemplate, objectMapper, TOPIC, acceptanceService);
        request = new CreateOrderRequest("AAPL", Side.BUY, 10L, new BigDecimal("185.50"));
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        savedOrder = new OrderResponse(42L, OWNER, "AAPL", Side.BUY, 10L,
                new BigDecimal("185.50"), OrderStatus.SUBMITTED, now, now, 0L);
    }

    // happy path, succsesfull submit order request and response
    @Test
    void submitOrderPublishesSavedOrderAndReturnsIt() throws Exception {
        when(orderService.createOrder(request, OWNER)).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(savedOrder)).thenReturn(PAYLOAD);
        when(kafkaTemplate.send(TOPIC, "42", PAYLOAD))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        OrderResponse response = submissionService.submitOrder(request, OWNER);

        assertSame(savedOrder, response);
        var sequence = inOrder(orderService, objectMapper, kafkaTemplate, acceptanceService);
        sequence.verify(orderService).createOrder(request, OWNER);
        sequence.verify(objectMapper).writeValueAsString(savedOrder);
        sequence.verify(kafkaTemplate).send(TOPIC, "42", PAYLOAD);
        sequence.verify(acceptanceService).acceptLater(42L);
        verifyNoMoreInteractions(orderService, objectMapper, kafkaTemplate);
    }

    // mimicks database commit failing -> no push to kafka
    @Test
    void submitOrderDoesNotPublishWhenDatabaseCreationFails() {
        RuntimeException failure = new IllegalStateException("Database unavailable");
        when(orderService.createOrder(request, OWNER)).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> submissionService.submitOrder(request, OWNER));

        assertSame(failure, thrown);
        verify(orderService).createOrder(request, OWNER);
        verifyNoInteractions(objectMapper, kafkaTemplate, acceptanceService);
    }

    @Test
    void submitOrderReturnsUnavailableWhenKafkaFails() throws Exception {
        when(orderService.createOrder(request, OWNER)).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(savedOrder)).thenReturn(PAYLOAD);
        RuntimeException failure = new IllegalStateException("Kafka unavailable");
        when(kafkaTemplate.send(TOPIC, "42", PAYLOAD))
                .thenReturn(CompletableFuture.failedFuture(failure));

        ResponseStatusException thrown = assertPublishFailure();

        assertInstanceOf(ExecutionException.class, thrown.getCause());
        assertSame(failure, thrown.getCause().getCause());
        verifyNoInteractions(acceptanceService);
    }

    private ResponseStatusException assertPublishFailure() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> submissionService.submitOrder(request, OWNER));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatusCode());
        assertEquals("Order 42 was saved but Kafka publishing was not confirmed", thrown.getReason());
        verify(orderService, times(1)).createOrder(request, OWNER);
        verify(kafkaTemplate, times(1)).send(TOPIC, "42", PAYLOAD);
        return thrown;
    }

}
