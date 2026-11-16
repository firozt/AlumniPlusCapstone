package com.alumniplus.order_service.controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;
import org.springframework.kafka.core.KafkaTemplate;
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
