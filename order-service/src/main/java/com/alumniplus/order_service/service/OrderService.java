package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.CancellationResult;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
import com.alumniplus.order_service.model.Instrument;
import com.alumniplus.order_service.model.OrderEntity;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.repository.InstrumentRepository;
import com.alumniplus.order_service.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final TransactionTemplate transactions; // used to make submit and cancel order transactions
    private final Executor acceptanceExecutor; // used to make accepting an order a background process
    private final long acceptanceDelayMs;

    public OrderService(OrderRepository orderRepository, InstrumentRepository instrumentRepository,
            OrderEventPublisher orderEventPublisher, PlatformTransactionManager transactionManager,
            @Qualifier("orderAcceptanceExecutor") Executor acceptanceExecutor,
            @Value("${app.orders.acceptance-delay-ms}") long acceptanceDelayMs) {
        if (acceptanceDelayMs < 0) {
            throw new IllegalArgumentException("Acceptance delay must not be negative");
        }
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.acceptanceExecutor = acceptanceExecutor;
        this.acceptanceDelayMs = acceptanceDelayMs;
    }

    public OrderResponse submitOrder(CreateOrderRequest request, String owner) {
        OrderResponse order = transactions.execute(status -> createOrder(request, owner));
        orderEventPublisher.publish(order);

        try {
            acceptanceExecutor.execute(() -> acceptLater(order.orderId()));
        } catch (RejectedExecutionException exception) {
            // keep the committed submission if the acceptance queue is full/ fails
        }
        return order;
    }

    public OrderResponse cancelOrder(Long orderId, String owner) {
        CancellationResult result = transactions.execute(status -> cancelInTransaction(orderId, owner));
        // check if order was actually changed
        if (result.changed()) {
            orderEventPublisher.publish(result.order());
        }
        return result.order();
    }

    // this should only be called via the executor so it doesnt stall the main thread
    void acceptLater(Long orderId) {
        try {
            Thread.sleep(acceptanceDelayMs);
            Optional<OrderResponse> accepted = transactions.execute(status -> acceptIfSubmitted(orderId));
            accepted.ifPresent(orderEventPublisher::publish);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ObjectOptimisticLockingFailureException exception) {
            // A concurrent update wins; do not overwrite it or publish acceptance.
        } catch (Exception exception) {
            // Background failures must not affect the completed submit request.
        }
    }

    // takes order request and inserts it into the database
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String owner) {
        // verify valid instrument
        Instrument instrument = isInstrumentExists(request.instrumentId());

        // create order
        OrderEntity order = new OrderEntity();
        order.setOwner(owner);
        order.setInstrument(instrument);
        order.setSide(request.side());
        order.setQuantity(request.quantity());
        order.setLimitPrice(request.limitPrice());
        order.setStatus(OrderStatus.SUBMITTED);

        // insert to DB
        return OrderResponse.from(orderRepository.save(order));
    }

    // verify the instrument they want to buy actually exists
    @Transactional(readOnly = true)
    public Instrument isInstrumentExists(String id) {
        Instrument instrument = instrumentRepository.findById(id)
                .filter(Instrument::isActive)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active instrument not found: " + id));

        return instrument;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForOwner(String owner) {
        return orderRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    private CancellationResult cancelInTransaction(Long orderId, String owner) {
        // verify owner
        if (owner == null || owner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing preferred_username");
        }
        // fetch row from db
        OrderEntity order = orderRepository.findById(orderId) .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
        if (!owner.equals(order.getOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel another user's order");
        }

        // check if its already cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new CancellationResult(OrderResponse.from(order), false);
        }

        // set to cancelled
        order.setStatus(OrderStatus.CANCELLED);
        try {
            // update timestamps/version and detect concurrent changes.
            // if this fails, concurrency issue and nothing is commited
            orderRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order changed while cancelling; reload and retry", exception);
        }
        return new CancellationResult(OrderResponse.from(order), true);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getSummaryForOwner(String owner) {
        return summarize(orderRepository.findByOwnerOrderByCreatedAtDesc(owner));
    }

    @Transactional
    public Optional<OrderResponse> acceptIfSubmitted(Long orderId) {
        Optional<OrderEntity> existing = orderRepository.findById(orderId);
        if (existing.isEmpty() || existing.get().getStatus() != OrderStatus.SUBMITTED) {
            return Optional.empty();
        }
        OrderEntity order = existing.get();
        order.setStatus(OrderStatus.ACCEPTED);
        // flush checks @Version and updates timestamps before building the response.
        orderRepository.flush();
        return Optional.of(OrderResponse.from(order));
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOverallSummary() {
        return summarize(orderRepository.findAll());
    }

    private OrderSummaryResponse summarize(List<OrderEntity> orders) {
        // include all statuses + requested value is quantity * limit price.
        long count = orders.stream().count();
        BigDecimal total = orders.stream()
                .map(order -> order.getLimitPrice().multiply(BigDecimal.valueOf(order.getQuantity()))) // limit * quantity
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderSummaryResponse(count, total);
    }
}
