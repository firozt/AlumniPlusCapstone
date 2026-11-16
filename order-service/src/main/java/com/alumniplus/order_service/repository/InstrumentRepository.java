package com.alumniplus.order_service.repository;

import com.alumniplus.order_service.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {

    List<Instrument> findByActiveTrue();
}
