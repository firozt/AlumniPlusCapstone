package com.alumniplus.activity_service.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderActivityEvent(
    Long orderId,
    String owner,
    String status,
    Instant updatedAt,
    Long version
) {}