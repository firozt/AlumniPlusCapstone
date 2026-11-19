package com.alumniplus.order_service.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
import org.springframework.security.core.Authentication;
import com.alumniplus.order_service.service.OrderService;
import com.alumniplus.order_service.service.OrderSubmissionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSubmissionService orderSubmissionService;

    public OrderController(OrderService orderService,OrderSubmissionService orderSubmissionService) {
        this.orderService = orderService;
        this.orderSubmissionService = orderSubmissionService;
    }

    @GetMapping("/")
    public String root() {
        return "working\n";
    }

    @PreAuthorize("hasRole('TRADER')")
    @GetMapping("/view")
    public List<OrderResponse> getUserOrders(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing preferred_username");
        }
        return orderService.getOrdersForOwner(username);
    }

    @PreAuthorize("hasRole('OPERATIONS')")
    @GetMapping("/view-all")
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PreAuthorize("hasAnyRole('TRADER', 'OPERATIONS')")
    @GetMapping("/summary")
    public OrderSummaryResponse getOrderSummary(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        // TODO: Use authorities to select overall totals for OPERATIONS.
        // TODO: For TRADER, restrict orders to the token's preferred_username.
        // TODO: Use Java Streams to calculate order count and sum(quantity *
        // limitPrice).
        // TODO: Return both values in OrderSummaryResponse; no grouping is needed.
        throw new UnsupportedOperationException("Order summary is not implemented yet");
    }

    @PreAuthorize("hasRole('TRADER')")
    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse submitOrder(@Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing preferred_username");
        }
        return orderSubmissionService.submitOrder(request, username);
    }

    @PreAuthorize("hasRole('TRADER')")
    @PostMapping("/cancel/{orderId}")
    public OrderResponse cancelOrder(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Jwt jwt) {
        // TODO: implement

        String username = jwt.getClaimAsString("preferred_username");
        return null;
    }

    // // TODO - should probably delete these at some point
    // @PostMapping("/test-kafka")
    // public CompletableFuture<Map<String, Object>> testKafka(
    // @RequestParam(defaultValue = "Kafka test message") String message) {
    // return kafkaTemplate.send("orders-test", message)
    // .thenApply(result -> {
    // var metadata = result.getRecordMetadata();
    // return Map.<String, Object>of(
    // "topic", metadata.topic(),
    // "partition", metadata.partition(),
    // "offset", metadata.offset(),
    // "message", message);
    // });
    // }
    //
    // @PostMapping("/test-db")
    // public OrderResponse insertDB(@RequestParam String instrumentId) {
    // CreateOrderRequest request = new CreateOrderRequest(
    // instrumentId,
    // Side.BUY,
    // 1L,
    // new BigDecimal("1.0000"));
    // return orderService.createOrder(request, "test-user");
    // }
}
