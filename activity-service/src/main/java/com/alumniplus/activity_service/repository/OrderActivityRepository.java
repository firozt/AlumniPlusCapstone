
package com.alumniplus.activity_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alumniplus.activity_service.model.OrderActivity;

public interface OrderActivityRepository
        extends JpaRepository<OrderActivity, String> {

    List<OrderActivity> findByOrderIdOrderByOccurredAtAsc(Long orderId);
}
