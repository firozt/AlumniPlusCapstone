package com.alumniplus.activity_service.consumer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.service.OrderActivityService;

import tools.jackson.databind.json.JsonMapper;

class OrderActivityConsumerTest {

    private OrderActivityService activityService;
    private OrderActivityConsumer consumer;

    @BeforeEach
    void setUp() {
        activityService = mock(OrderActivityService.class);

        consumer = new OrderActivityConsumer(
            JsonMapper.builder().build(),
            activityService
        );
    }

    private String createMessage(String status, long version) {
        return """
            {
                "orderId": 49,
                "owner": "team-1-trader1",
                "instrumentId": "AAPL",
                "side": "BUY",
                "quantity": 10,
                "limitPrice": 185.50,
                "status": "%s",
                "createdAt": "2026-11-25T10:00:00Z",
                "updatedAt": "2026-11-25T10:01:00Z",
                "version": %d
            }
            """.formatted(status, version);
    }

    @Test
    void consumesSubmittedEvent() {
        consumer.consume(createMessage("SUBMITTED", 0));

        ArgumentCaptor<OrderActivity> captor =
            ArgumentCaptor.forClass(OrderActivity.class);

        verify(activityService).recordActivity(captor.capture());

        OrderActivity activity = captor.getValue();

        assertEquals(49L, activity.getOrderId());
        assertEquals("SUBMITTED", activity.getEventType());
        assertEquals("team-1-trader1", activity.getOwner());

        assertEquals(
            LocalDateTime.parse("2026-11-25T10:01:00"),
            activity.getOccurredAt()
        );

        assertNotNull(activity.getReceivedAt());

        String expectedId = UUID.nameUUIDFromBytes(
            "49:0".getBytes(StandardCharsets.UTF_8)
        ).toString();

        assertEquals(expectedId, activity.getEventId());
    }

    @Test
    void consumesAcceptedEvent() {
        consumer.consume(createMessage("ACCEPTED", 1));

        ArgumentCaptor<OrderActivity> captor =
            ArgumentCaptor.forClass(OrderActivity.class);

        verify(activityService).recordActivity(captor.capture());

        assertEquals(
            "ACCEPTED",
            captor.getValue().getEventType()
        );
    }

    @Test
    void consumesCancelledEvent() {
        consumer.consume(createMessage("CANCELLED", 2));

        ArgumentCaptor<OrderActivity> captor =
            ArgumentCaptor.forClass(OrderActivity.class);

        verify(activityService).recordActivity(captor.capture());

        assertEquals(
            "CANCELLED",
            captor.getValue().getEventType()
        );
    }

    @Test
    void duplicateMessagesGenerateSameEventId() {
        String message = createMessage("SUBMITTED", 0);

        consumer.consume(message);
        consumer.consume(message);

        ArgumentCaptor<OrderActivity> captor =
            ArgumentCaptor.forClass(OrderActivity.class);

        verify(activityService, times(2))
            .recordActivity(captor.capture());

        assertEquals(
            captor.getAllValues().get(0).getEventId(),
            captor.getAllValues().get(1).getEventId()
        );
    }

    @Test
    void differentVersionsGenerateDifferentEventIds() {
        consumer.consume(createMessage("SUBMITTED", 0));
        consumer.consume(createMessage("ACCEPTED", 1));
        consumer.consume(createMessage("CANCELLED", 2));

        ArgumentCaptor<OrderActivity> captor =
            ArgumentCaptor.forClass(OrderActivity.class);

        verify(activityService, times(3))
            .recordActivity(captor.capture());

        long distinctIds = captor.getAllValues()
            .stream()
            .map(OrderActivity::getEventId)
            .distinct()
            .count();

        assertEquals(3L, distinctIds);
    }

    @Test
    void rejectsInvalidStatus() {
        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(
                createMessage("INVALID", 0)
            )
        );

        verifyNoInteractions(activityService);
    }

    @Test
    void rejectsNegativeVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(
                createMessage("SUBMITTED", -1)
            )
        );

        verifyNoInteractions(activityService);
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume("{}")
        );

        verifyNoInteractions(activityService);
    }
}