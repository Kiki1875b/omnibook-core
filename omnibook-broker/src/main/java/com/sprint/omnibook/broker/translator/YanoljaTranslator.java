package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload;
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper;
import org.springframework.stereotype.Component;

@Component
public class YanoljaTranslator extends AbstractTranslator<YanoljaPayload> {

    private final ReservationEventMapper mapper;

    public YanoljaTranslator(ObjectMapper objectMapper, ReservationEventMapper mapper) {
        super(objectMapper);
        this.mapper = mapper;
    }

    @Override
    protected PlatformType getPlatformType() {
        return PlatformType.YANOLJA;
    }

    @Override
    protected Class<YanoljaPayload> getDtoClass() {
        return YanoljaPayload.class;
    }

    @Override
    protected ReservationEvent mapToEvent(YanoljaPayload dto, TranslationContext ctx) {
        return mapper.fromYanolja(dto, ctx);
    }
}
