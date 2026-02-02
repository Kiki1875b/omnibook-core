package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper
import org.springframework.stereotype.Component

@Component
class YanoljaTranslator(
    objectMapper: ObjectMapper,
    private val mapper: ReservationEventMapper
) : AbstractTranslator<YanoljaPayload>(objectMapper) {

    override fun getPlatformType(): PlatformType = PlatformType.YANOLJA

    override fun getDtoClass(): Class<YanoljaPayload> = YanoljaPayload::class.java

    override fun mapToEvent(dto: YanoljaPayload, ctx: TranslationContext): ReservationEvent =
        mapper.fromYanolja(dto, ctx)
}
