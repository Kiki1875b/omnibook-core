package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload;
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper;
import org.springframework.stereotype.Component;

@Component
public class YeogieottaeTranslator extends AbstractTranslator<YeogieottaePayload> {

    private final ReservationEventMapper mapper;

    public YeogieottaeTranslator(ObjectMapper objectMapper, ReservationEventMapper mapper) {
        super(objectMapper);
        this.mapper = mapper;
    }

    @Override
    protected PlatformType getPlatformType() {
        return PlatformType.YEOGIEOTTAE;
    }

    @Override
    protected Class<YeogieottaePayload> getDtoClass() {
        return YeogieottaePayload.class;
    }

    @Override
    protected ReservationEvent mapToEvent(YeogieottaePayload dto, TranslationContext ctx) {
        return mapper.fromYeogieottae(dto, ctx);
    }
}
