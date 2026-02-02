package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper
import org.springframework.stereotype.Component

@Component
class AirbnbTranslator(
    objectMapper: ObjectMapper,
    private val mapper: ReservationEventMapper
) : AbstractTranslator<AirbnbPayload>(objectMapper) {

    override fun getPlatformType(): PlatformType = PlatformType.AIRBNB

    override fun getDtoClass(): Class<AirbnbPayload> = AirbnbPayload::class.java

    override fun mapToEvent(dto: AirbnbPayload, ctx: TranslationContext): ReservationEvent =
        mapper.fromAirbnb(dto, ctx)
}
