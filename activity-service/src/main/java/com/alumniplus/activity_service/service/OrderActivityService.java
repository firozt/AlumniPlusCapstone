
package com.alumniplus.activity_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;

@Service
public class OrderActivityService {

    private final OrderActivityRepository repository;

    public OrderActivityService(OrderActivityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordActivity(OrderActivity activity) {
        repository.saveAndFlush(activity);
    }

    @Transactional(readOnly = true)
    public List<OrderActivity> getOrderHistory(Long orderId) {
        return repository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }
}
