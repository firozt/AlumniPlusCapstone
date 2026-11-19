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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock // Replaces the real database repository.
    private OrderRepository orderRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks // Creates the service with the mocked repositories.
    private OrderService orderService;

    @Test
    void createOrderSavesSubmittedOrderForOwner() {
        // Sets up data mocking database
        Instrument instrument = new Instrument();
        instrument.setId("AAPL");
        instrument.setCurrency("GBP");
        instrument.setActive(true);

        // when we query 'AAPL' return the instrument created above
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
