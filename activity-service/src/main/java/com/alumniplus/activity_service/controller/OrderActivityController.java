
package com.alumniplus.activity_service.controller;

import java.util.List;

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
            @PathVariable Long orderId) {

        return service.getOrderHistory(orderId);
    }
}
