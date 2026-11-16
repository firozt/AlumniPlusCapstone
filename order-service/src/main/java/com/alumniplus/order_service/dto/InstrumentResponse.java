package com.alumniplus.order_service.dto;

import com.alumniplus.order_service.model.Instrument;

public record InstrumentResponse(String instrumentId, String name, String currency, boolean active) {

  public static InstrumentResponse from(Instrument i) {
    return new InstrumentResponse(
        i.getId(), i.getName(), i.getCurrency(), i.isActive());
  }

}
