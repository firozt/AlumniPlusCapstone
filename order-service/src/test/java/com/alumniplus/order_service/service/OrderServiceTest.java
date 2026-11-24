package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
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
import org.mockito.InjectMocks;
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

    @InjectMocks 
    private OrderService orderService;

    @Test
    void submittedOrderIsAccepted() {
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.acceptIfSubmitted(10L).orElseThrow();

        assertEquals(OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(OrderStatus.ACCEPTED, response.status());
        verify(orderRepository).flush();
    }

    @Test
    void cancelledOrderIsNotAccepted() {
        OrderEntity order = cancellationOrder(OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository, never()).flush();
    }

    @Test
    void alreadyAcceptedOrderIsNotChangedAgain() {
        OrderEntity order = cancellationOrder(OrderStatus.ACCEPTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        verify(orderRepository, never()).flush();
    }

    @Test
    void missingOrderIsSkippedByAcceptance() {
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());
        assertTrue(orderService.acceptIfSubmitted(10L).isEmpty());
        verify(orderRepository, never()).flush();
    }

    @Test
    void acceptancePropagatesConcurrentUpdateForRollback() {
        when(orderRepository.findById(10L))
                .thenReturn(Optional.of(cancellationOrder(OrderStatus.SUBMITTED)));
        var conflict = new ObjectOptimisticLockingFailureException(OrderEntity.class, 10L);
        doThrow(conflict).when(orderRepository).flush();

        assertSame(conflict, assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> orderService.acceptIfSubmitted(10L)));
    }

    @Test
    void createOrderRejectsMissingInstrument() {
        when(instrumentRepository.findById("AAPL")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> orderService.createOrder(sampleRequest(), "team-1-trader1"));

        verify(instrumentRepository).findById("AAPL");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrderRejectsInactiveInstrument() {
        Instrument instrument = new Instrument();
        instrument.setId("AAPL");
        instrument.setActive(false);
        when(instrumentRepository.findById("AAPL")).thenReturn(Optional.of(instrument));

        assertThrows(EntityNotFoundException.class,
                () -> orderService.createOrder(sampleRequest(), "team-1-trader1"));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void ownerCanCancelOrder() {
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelOrder(10L, "team-1-trader1");

        assertAll(
                () -> assertEquals(OrderStatus.CANCELLED, order.getStatus()),
                () -> assertEquals(OrderStatus.CANCELLED, response.status()),
                () -> assertEquals(10L, response.orderId()),
                () -> assertEquals("team-1-trader1", response.owner()));
        verify(orderRepository).flush();
    }

    @Test
    void cancelOrderRejectsDifferentOwner() {
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader2"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(OrderStatus.SUBMITTED, order.getStatus());
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrderRejectsMissingOrder() {
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancellingAlreadyCancelledOrderLeavesItUnchanged() {
        OrderEntity order = cancellationOrder(OrderStatus.CANCELLED);
        OrderResponse original = OrderResponse.from(order);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelOrder(10L, "team-1-trader1");

        assertEquals(original, response);
        assertEquals(original, OrderResponse.from(order));
        verify(orderRepository).findById(10L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrderReturnsConflictForConcurrentUpdate() {
        OrderEntity order = cancellationOrder(OrderStatus.SUBMITTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        var conflict = new ObjectOptimisticLockingFailureException(OrderEntity.class, 10L);
        doThrow(conflict).when(orderRepository).flush();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(10L, "team-1-trader1"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertSame(conflict, exception.getCause());
        verify(orderRepository).flush();
        // Database rollback and real @Version conflicts require an integration test.
    }

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

    @Test
    void createOrderSavesSubmittedOrderForOwner() {
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
        OrderResponse response = orderService.createOrder(request, "student14");

        // Assert: inspect the order passed to the repository and the response.
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
}
