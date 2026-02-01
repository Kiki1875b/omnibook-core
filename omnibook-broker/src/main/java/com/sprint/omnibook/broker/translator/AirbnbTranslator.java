package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload;
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper;
import org.springframework.stereotype.Component;

@Component
public class AirbnbTranslator extends AbstractTranslator<AirbnbPayload> {

    private final ReservationEventMapper mapper;

    public AirbnbTranslator(ObjectMapper objectMapper, ReservationEventMapper mapper) {
        super(objectMapper);
        this.mapper = mapper;
    }

    @Override
    protected PlatformType getPlatformType() {
        return PlatformType.AIRBNB;
    }

    @Override
    protected Class<AirbnbPayload> getDtoClass() {
        return AirbnbPayload.class;
    }

    @Override
    protected ReservationEvent mapToEvent(AirbnbPayload dto, TranslationContext ctx) {
        return mapper.fromAirbnb(dto, ctx);
    }
}
