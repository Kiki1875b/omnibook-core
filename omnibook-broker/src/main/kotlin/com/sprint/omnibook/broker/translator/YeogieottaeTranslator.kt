package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper
import org.springframework.stereotype.Component

@Component
class YeogieottaeTranslator(
    objectMapper: ObjectMapper,
    private val mapper: ReservationEventMapper
) : AbstractTranslator<YeogieottaePayload>(objectMapper) {

    override fun getPlatformType(): PlatformType = PlatformType.YEOGIEOTTAE

    override fun getDtoClass(): Class<YeogieottaePayload> = YeogieottaePayload::class.java

    override fun mapToEvent(dto: YeogieottaePayload, ctx: TranslationContext): ReservationEvent =
        mapper.fromYeogieottae(dto, ctx)
}
