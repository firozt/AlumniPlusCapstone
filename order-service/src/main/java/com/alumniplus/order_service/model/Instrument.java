package com.alumniplus.order_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.YesNoConverter;

@Entity
@Table(name = "INSTRUMENTS")
@Getter
@Setter
@NoArgsConstructor
public class Instrument {

    @Id
    @Column(name = "INSTRUMENT_ID")
    private Long id;

    @Column(name = "SYMBOL", nullable = false, length = 10)
    private String symbol;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "ASSET_CLASS", nullable = false, length = 20)
    private String assetClass;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "ACTIVE", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    private boolean active;
}
