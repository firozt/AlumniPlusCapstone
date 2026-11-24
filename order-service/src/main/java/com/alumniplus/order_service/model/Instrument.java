package com.alumniplus.order_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.YesNoConverter;

@Entity
@Table(name = "INSTRUMENT")
@Getter
@Setter
@NoArgsConstructor
public class Instrument {

    @Id
    @Column(name = "INSTRUMENT_ID")
    private String id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CURRENCY", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "ACTIVE", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    private boolean active = true;
}
