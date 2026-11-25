package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.Instrument;
import com.alumniplus.order_service.model.OrderEntity;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import com.alumniplus.order_service.repository.InstrumentRepository;
import com.alumniplus.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.concurrent.Executor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock 
    private OrderRepository orderRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private Executor acceptanceExecutor;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactionManager.getTransaction(any()))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        orderService = new OrderService(orderRepository, instrumentRepository, orderEventPublisher,
                transactionManager, acceptanceExecutor, 0);
    }

    //happy path
    @Test
    void submittedOrderIsAccepted() {
        // given -> a SUBMITTED order returned by the repository
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> acceptIfSubmitted is called
        OrderResponse response = orderService.acceptIfSubmitted(10L).orElseThrow();

        // then -> change the entity and response to ACCEPTED and flush
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(OrderStatus.ACCEPTED, response.status());
        verify(orderRepository).flush();
    }

    @Test
    void cancelledOrderIsNotAccepted() {
        // given -> a CANCELLED order returned by the repository
        OrderEntity order = cancellationOrder(OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> acceptIfSubmitted is called
        // then -> return empty, keep CANCELLED, and do not flush
        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository, never()).flush();
    }

    @Test
    void alreadyAcceptedOrderIsNotChangedAgain() {
        // given -> an ACCEPTED order returned by the repository
        OrderEntity order = cancellationOrder(OrderStatus.ACCEPTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> acceptIfSubmitted is called again
        // then -> return empty without another flush
        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        verify(orderRepository, never()).flush();
    }

    @Test
    void missingOrderIsSkippedByAcceptance() {
        // given -> an order lookup that returns empty
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());
        // when -> acceptIfSubmitted is called
        // then -> return empty without flushing
        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        verify(orderRepository, never()).flush();
    }

    @Test
    void createOrderRejectsMissingInstrument() {
        // given -> an instrument lookup that returns empty
        when(instrumentRepository.findById("AAPL")).thenReturn(Optional.empty());

        // when -> createOrder is called
        // then -> throw EntityNotFoundException without saving an order
        assertThrows(EntityNotFoundException.class,
                () -> orderService.createOrder(sampleRequest(), "team-1-trader1"));

        verify(instrumentRepository).findById("AAPL");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrderRejectsInactiveInstrument() {
        // given -> an inactive instrument returned by the repository
        Instrument instrument = new Instrument();
        instrument.setId("AAPL");
        instrument.setActive(false);
        when(instrumentRepository.findById("AAPL")).thenReturn(Optional.of(instrument));

        // when -> createOrder is called
        // then -> throw EntityNotFoundException without saving an order
        assertThrows(EntityNotFoundException.class,
                () -> orderService.createOrder(sampleRequest(), "team-1-trader1"));

        verifyNoInteractions(orderRepository);
    }

    //happy path
    @Test
    void ownerCanCancelOrder() {
        // given -> a SUBMITTED order belonging to the caller
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> the owner cancels the order
        OrderResponse response = orderService.cancelOrder(10L, "team-1-trader1");
        // then -> return CANCELLED, flush the update, and publish it
        verify(orderEventPublisher).publish(response);
        assertAll(
                () -> assertEquals(OrderStatus.CANCELLED, order.getStatus()),
                () -> assertEquals(OrderStatus.CANCELLED, response.status()),
                () -> assertEquals(10L, response.orderId()),
                () -> assertEquals("team-1-trader1", response.owner()));
        verify(orderRepository).flush();
    }

    @Test
    void cancelOrderRejectsDifferentOwner() {
        // given -> an order belonging to another trader
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> the caller tries to cancel it
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader2"));

        // then -> return 403 without changing or flushing the order
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(OrderStatus.SUBMITTED, order.getStatus());
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrderRejectsMissingOrder() {
        // given -> an order lookup that returns empty
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        // when -> the caller tries to cancel it
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader1"));

        // then -> return 404 without flushing
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancellingAlreadyCancelledOrderLeavesItUnchanged() {
        // given -> a CANCELLED order belonging to the caller
        OrderEntity order = cancellationOrder(OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when -> the owner cancels it again
        OrderResponse response = orderService.cancelOrder(10L, "team-1-trader1");
        // then -> return the unchanged order without publishing or flushing
        verifyNoInteractions(orderEventPublisher);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrderReturnsConflictForConcurrentUpdate() {
        // given -> an owned order whose flush raises an optimistic-lock conflict
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        var conflict = new ObjectOptimisticLockingFailureException(OrderEntity.class, 10L);
        doThrow(conflict).when(orderRepository).flush();

        // when -> the owner tries to cancel it
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader1"));

        // then -> return 409 with the conflict as its cause
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertSame(conflict, exception.getCause());
        verify(orderRepository).flush();
        // Database rollback and real @Version conflicts require an integration test.
    }


    //happy path
    @Test
    void createOrderSavesSubmittedOrderForOwner() {
        // given -> an active instrument, valid request, and mocked generated ID
        // set up data mocking db
        Instrument instrument = new Instrument();
        instrument.setId("AAPL");
        instrument.setCurrency("GBP");
        instrument.setActive(true);

        // when we query AAPL return the instrument created above
        when(instrumentRepository.findById("AAPL")) .thenReturn(Optional.of(instrument));
        
       // when we save an order to the orders db, assign it id 42 and return it 
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(42L);
            return order;
        });

        CreateOrderRequest request = new CreateOrderRequest(
                "AAPL", Side.BUY, 10L, new BigDecimal("185.50"));

        // Act: call the service directly.
        // when -> createOrder is called for student14
        OrderResponse response = orderService.createOrder(request, "student14");

        // Assert: inspect the order passed to the repository and the response.
        // then -> save the correct fields and return the generated ID with SUBMITTED
        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity saved = captor.getValue();
        assertAll(
                () -> assertEquals("student14", saved.getOwner()),
                () -> assertSame(instrument, saved.getInstrument()),
                () -> assertEquals(Side.BUY, saved.getSide()),
                () -> assertEquals(10L, saved.getQuantity()),
                () -> assertEquals(new BigDecimal("185.50"), saved.getLimitPrice()),
                () -> assertEquals(OrderStatus.SUBMITTED, saved.getStatus()),
                () -> assertEquals(42L, response.orderId()),
                () -> assertEquals("student14", response.owner()),
                () -> assertEquals("AAPL", response.instrumentId()),
                () -> assertEquals(OrderStatus.SUBMITTED, response.status()));
    }

    // HELPERS 
    //
    private CreateOrderRequest sampleRequest() {
        return new CreateOrderRequest("AAPL", Side.BUY, 10L, new BigDecimal("185.50"));
    }

    private OrderEntity cancellationOrder(OrderStatus status) {
        Instrument instrument = new Instrument();
        instrument.setId("AAPL");
        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setOwner("team-1-trader1");
        order.setInstrument(instrument);
        order.setSide(Side.BUY);
        order.setQuantity(10L);
        order.setLimitPrice(new BigDecimal("185.50"));
        order.setStatus(status);
        order.setVersion(0L);
        return order;
    }
}
