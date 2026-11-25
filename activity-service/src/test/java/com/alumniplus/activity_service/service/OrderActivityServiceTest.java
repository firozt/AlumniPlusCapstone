
package com.alumniplus.activity_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;

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

    @Test
    void shouldIgnoreDuplicateEvent() {

        String eventId = UUID.randomUUID().toString();

        OrderActivity original = new OrderActivity(
                eventId,
                1001L,
                "SUBMITTED",
                LocalDateTime.now(),
                "trader1"
        );

        // First delivery
        service.recordActivity(original);

        // Simulate the same event ID arriving again,
        // but with different data.
        OrderActivity duplicate = new OrderActivity(
                eventId,
                1001L,
                "CANCELLED",
                LocalDateTime.now(),
                "trader2"
        );

        // Second delivery should be ignored.
        assertDoesNotThrow(() -> service.recordActivity(duplicate));

        // Verify the original event was not overwritten.
        OrderActivity saved = repository.findById(eventId)
                .orElseThrow();

        assertEquals("SUBMITTED", saved.getEventType());
        assertEquals("trader1", saved.getOwner());

        // Verify only one row exists for this event ID.
        assertEquals(
                1,
                repository.findAllById(List.of(eventId)).size()
        );
    }
}
