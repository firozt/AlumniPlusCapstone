package com.alumniplus.activity_service.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;

@Service
public class OrderActivityService {

    private final OrderActivityRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public OrderActivityService(
            OrderActivityRepository repository,
            JdbcTemplate jdbcTemplate) {

        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void recordActivity(OrderActivity activity) {

        if (activity.getReceivedAt() == null) {
            activity.setReceivedAt(LocalDateTime.now());
        }

        String sql = """
            INSERT INTO ORDER_ACTIVITY
                (EVENT_ID, ORDER_ID, EVENT_TYPE,
                 OWNER, OCCURRED_AT, RECEIVED_AT)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try {
            jdbcTemplate.update(
                    sql,
                    activity.getEventId(),
                    activity.getOrderId(),
                    activity.getEventType(),
                    activity.getOwner(),
                    Timestamp.valueOf(activity.getOccurredAt()),
                    Timestamp.valueOf(activity.getReceivedAt())
            );

        } catch (DuplicateKeyException ex) {

            if (repository.existsById(activity.getEventId())) {
                return;
            }

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderActivity> getOrderHistory(Long orderId) {

        return repository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderActivity> getAuthorizedOrderHistory(
            Long orderId,
            String username,
            boolean isOperations) {

        List<OrderActivity> history = getOrderHistory(orderId);

        // Operations users can view all order histories.
        if (isOperations) {
            return history;
        }

        // Traders must have a valid username.
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token is missing preferred_username"
            );
        }

        // Establish ownership from submission events.
        List<OrderActivity> submissions = history.stream()
                .filter(activity ->
                        "SUBMITTED".equals(activity.getEventType()))
                .toList();

        // Deny access if ownership cannot be established.
        boolean ownsOrder = !submissions.isEmpty()
                && submissions.stream()
                        .allMatch(activity ->
                                username.equals(activity.getOwner()));

        if (!ownsOrder) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to view this order"
            );
        }

        return history;
    }
}