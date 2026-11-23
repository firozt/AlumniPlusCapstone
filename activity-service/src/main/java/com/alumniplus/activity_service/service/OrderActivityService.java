
package com.alumniplus.activity_service.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        } catch (DuplicateKeyException e) {
            // An event with this ID was already recorded.
            // Ignore it without changing the existing row.
            if (repository.existsById(activity.getEventId())) {
                return;
            }

            // A different unique constraint was violated.
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderActivity> getOrderHistory(Long orderId) {
        return repository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }
}
