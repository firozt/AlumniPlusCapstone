package com.alumniplus.order_service.controller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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
}
