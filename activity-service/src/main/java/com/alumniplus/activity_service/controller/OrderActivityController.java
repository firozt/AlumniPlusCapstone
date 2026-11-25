package com.alumniplus.activity_service.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.service.OrderActivityService;

@RestController
@RequestMapping("/api/activity")
public class OrderActivityController {

    private final OrderActivityService service;

    public OrderActivityController(OrderActivityService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}")
    public List<OrderActivity> getOrderHistory(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        String username = jwt.getClaimAsString(
                "preferred_username"
        );

        boolean isOperations = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_OPERATIONS".equals(
                                authority.getAuthority()
                        )
                );

        return service.getAuthorizedOrderHistory(
                orderId,
                username,
                isOperations
        );
    }
}