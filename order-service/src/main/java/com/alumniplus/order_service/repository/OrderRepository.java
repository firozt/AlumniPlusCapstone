package com.alumniplus.order_service.repository;

import com.alumniplus.order_service.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByOwnerOrderByCreatedAtDesc(String owner);
}
