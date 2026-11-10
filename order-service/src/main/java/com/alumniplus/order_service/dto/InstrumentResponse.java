package com.alumniplus.order_service.dto;

import com.alumniplus.order_service.model.Instrument;

public record InstrumentResponse( Long instrumentId, String symbol, String name, String assetClass, String currency) {

  public static InstrumentResponse from(Instrument i) {
    return new InstrumentResponse(
        i.getId(), i.getSymbol(), i.getName(), i.getAssetClass(), i.getCurrency());
  }

}
