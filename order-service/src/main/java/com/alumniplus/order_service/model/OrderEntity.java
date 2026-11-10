package com.alumniplus.order_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ORDERS")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_gen")
    @SequenceGenerator(name = "order_gen", sequenceName = "ORDER_SEQ", allocationSize = 1)
    @Column(name = "ORDER_ID")
    private Long id;

    @Column(name = "OWNER_USERNAME", nullable = false, length = 50)
    private String ownerUsername;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "INSTRUMENT_ID", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "SIDE", nullable = false, length = 4)
    private Side side;

    @Column(name = "QUANTITY", nullable = false, precision = 12, scale = 0)
    private Long quantity;

    @Column(name = "LIMIT_PRICE", nullable = false, precision = 12, scale = 4)
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
}
