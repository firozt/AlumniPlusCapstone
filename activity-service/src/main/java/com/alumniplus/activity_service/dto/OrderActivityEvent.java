
package com.alumniplus.activity_service.dto;

import java.time.Instant;

// DTO uses eventTime and actor instead of occurredAt and owner to 
// match the message received from the Order Service.
// While the model uses occurredAt and owner to match the database column names.
public record OrderActivityEvent(
    String eventId,
    Long orderId,
    String eventType,
    Instant eventTime,
    String actor
) {
}
