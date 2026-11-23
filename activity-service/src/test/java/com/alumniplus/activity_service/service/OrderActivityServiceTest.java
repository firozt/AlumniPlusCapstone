
package com.alumniplus.activity_service.service;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderActivityServiceTest {

    @Autowired
    private OrderActivityService service;

    @Autowired
    private OrderActivityRepository repository;

    @Test
    void shouldSaveAndRetrieveActivity() {

        String eventId = UUID.randomUUID().toString();

        OrderActivity activity = new OrderActivity(
                eventId,
                1001L,
                "SUBMITTED",
                LocalDateTime.now(),
                "trader1"
        );

        service.recordActivity(activity);

        OrderActivity saved = repository.findById(eventId)
                .orElseThrow();

        assertEquals("SUBMITTED", saved.getEventType());
        assertEquals(1001L, saved.getOrderId());
        assertEquals("trader1", saved.getOwner());

        assertNotNull(saved.getOccurredAt());
        assertNotNull(saved.getReceivedAt());
    }
}
