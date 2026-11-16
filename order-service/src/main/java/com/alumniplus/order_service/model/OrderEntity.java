package com.alumniplus.order_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "TRADE_ORDER")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ORDER_ID")
  private Long id;

  @Column(name = "OWNER", nullable = false)
  private String owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "INSTRUMENT_ID", nullable = false, referencedColumnName = "INSTRUMENT_ID")
  private Instrument instrument;

  @Enumerated(EnumType.STRING)
  @Column(name = "SIDE", nullable = false, length = 4)
  private Side side;

  @Column(name = "QUANTITY", nullable = false, precision = 12, scale = 0)
  @Positive
  private Long quantity;

  @Column(name = "LIMIT_PRICE", nullable = false, precision = 12, scale = 4)
  @Positive
  private BigDecimal limitPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 10)
  private OrderStatus status = OrderStatus.SUBMITTED;

  @Column(name = "CREATED_AT", nullable = false)
  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  private Instant createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  private Instant updatedAt;

  @Version
  @Column(name = "VERSION", nullable = false)
  private Long version;

  @PrePersist
  private void applySchemaDefaults() {
    Instant now = Instant.now();
    if (status == null) {
      status = OrderStatus.SUBMITTED;
    }
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  private void updateTimestamp() {
    updatedAt = Instant.now();
  }
}
