package com.alumniplus.order_service.service;

import com.alumniplus.order_service.dto.InstrumentResponse;
import com.alumniplus.order_service.repository.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> getActiveInstruments() {
        return instrumentRepository.findByActiveTrue().stream()
                .map(InstrumentResponse::from)
                .toList();
    }
}
