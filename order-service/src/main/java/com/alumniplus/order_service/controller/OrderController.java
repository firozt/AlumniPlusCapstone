package com.alumniplus.order_service.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;


import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
import org.springframework.security.core.Authentication;
import com.alumniplus.order_service.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
        boolean isOperations = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OPERATIONS"));
        if (isOperations) {
            return orderService.getOverallSummary();
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing preferred_username");
        }
        return orderService.getSummaryForOwner(username);
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
        return orderService.submitOrder(request, username);
    }

    @PreAuthorize("hasRole('TRADER')")
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        return ResponseEntity.ok(orderService.cancelOrder(orderId, username));
    }

}
