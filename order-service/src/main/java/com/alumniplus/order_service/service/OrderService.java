package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.Instrument;
import com.alumniplus.order_service.model.OrderEntity;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.repository.InstrumentRepository;
import com.alumniplus.order_service.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;

    public OrderService(OrderRepository orderRepository, InstrumentRepository instrumentRepository) {
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String owner) {
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .filter(Instrument::isActive)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active instrument not found: " + request.instrumentId()));


        OrderEntity order = new OrderEntity();
        order.setOwner(owner);
        order.setInstrument(instrument);
        order.setSide(request.side());
        order.setQuantity(request.quantity());
        order.setLimitPrice(request.limitPrice());
        order.setStatus(OrderStatus.SUBMITTED);
        
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForOwner(String owner) {
        return orderRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
