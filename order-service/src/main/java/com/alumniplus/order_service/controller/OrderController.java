package com.alumniplus.order_service.controller;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.Side;
import com.alumniplus.order_service.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderService orderService;

    public OrderController(KafkaTemplate<String, String> kafkaTemplate, OrderService orderService) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderService = orderService;
    }

    // TODO: Write the endpoints here

  @GetMapping("/")
  public String root() {

    return "working";
  }


  @PreAuthorize("hasRole('TRADERS')")
  @GetMapping("/orders")
  public List<OrderResponse> getUserOrders(@AuthenticationPrincipal Jwt jwt) {
      String username = jwt.getClaimAsString("preferred_username");
      if (username == null || username.isBlank()) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing preferred_username");
      }
      // return orderService.getOrdersForOwner(username);
      System.out.println("[LOGS] Get Orders for studnet14");
      return orderService.getOrdersForOwner("studnet14");
  }


  @PreAuthorize("hasRole('OPERATIONS')")
  @GetMapping("/all-orders")
  public Map<String, Object> getAllOrders() {

      return null;
  }


  // TODO - should probably delete these at some point
  @PostMapping("/test-kafka")
  public CompletableFuture<Map<String, Object>> testKafka(
      @RequestParam(defaultValue = "Kafka test message") String message) {
    return kafkaTemplate.send("orders-test", message)
        .thenApply(result -> {
          var metadata = result.getRecordMetadata();
          return Map.<String, Object>of(
              "topic", metadata.topic(),
              "partition", metadata.partition(),
              "offset", metadata.offset(),
              "message", message);
        });
  }

  @PostMapping("/test-db")
  public OrderResponse insertDB(@RequestParam String instrumentId) {
      CreateOrderRequest request = new CreateOrderRequest(
          instrumentId,
          Side.BUY,
          1L,
          new BigDecimal("1.0000"));
      return orderService.createOrder(request, "test-user");
  }
}
