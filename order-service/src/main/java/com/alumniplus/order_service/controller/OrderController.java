package com.alumniplus.order_service.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // TODO: write the endpoints here

  @GetMapping("/")
  public String root() {
    return "working";
  }
}
