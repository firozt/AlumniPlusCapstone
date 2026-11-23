
package com.alumniplus.activity_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ORDER_ACTIVITY")
public class OrderActivity {

    @Id
    @Column(name = "EVENT_ID", length = 36)
    private String eventId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 10)
    private String eventType;

    @Column(name = "OWNER", nullable = false, length = 100)
    private String owner;

    @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "RECEIVED_AT", nullable = false)
    private LocalDateTime receivedAt;

    public OrderActivity() {
    }

    public OrderActivity(
            String eventId,
            Long orderId,
            String eventType,
            LocalDateTime occurredAt,
            String owner) {

        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.owner = owner;
        this.receivedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
