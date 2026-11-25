
package com.alumniplus.activity_service.consumer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.service.OrderActivityService;

import tools.jackson.databind.json.JsonMapper;

class OrderActivityConsumerTest {

    @Test
    void shouldConsumeOrderActivityEvent() {

        OrderActivityService service =
                mock(OrderActivityService.class);

        OrderActivityConsumer consumer =
                new OrderActivityConsumer(
                    service,
                    JsonMapper.builder().build()
                );

        String message = """
            {
                "eventId": "6f1c2b7e-3a54-4c8e-9d0a-2b8f1e5a7c11",
                "orderId": 1001,
                "eventType": "SUBMITTED",
                "eventTime": "2026-11-23T10:00:00Z",
                "actor": "trader1"
            }
            """;

        consumer.consume(message);

        ArgumentCaptor<OrderActivity> captor =
                ArgumentCaptor.forClass(OrderActivity.class);

        verify(service, times(1))
                .recordActivity(captor.capture());

        OrderActivity activity = captor.getValue();

        assertEquals(
                "6f1c2b7e-3a54-4c8e-9d0a-2b8f1e5a7c11",
                activity.getEventId()
        );

        assertEquals(1001L, activity.getOrderId());
        assertEquals("SUBMITTED", activity.getEventType());
        assertEquals("trader1", activity.getOwner());

        assertEquals(
                LocalDateTime.of(2026, 11, 23, 10, 0),
                activity.getOccurredAt()
        );

        assertNotNull(activity.getReceivedAt());
    }
}
