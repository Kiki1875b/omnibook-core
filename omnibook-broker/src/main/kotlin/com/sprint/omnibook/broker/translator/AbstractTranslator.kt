package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent

/**
 * Translator 공통 로직을 담은 추상 클래스.
 *
 * 담당:
 * - JSON → DTO 파싱 (Jackson)
 * - try-catch 래핑 및 TranslationException 변환
 * - TranslationContext 생성 및 Mapper 호출
 *
 * 자식 클래스 담당:
 * - DTO 타입 지정
 * - MapStruct Mapper 호출
 */
abstract class AbstractTranslator<T : Any>(
    protected val objectMapper: ObjectMapper
) : PayloadTranslator {

    final override fun translate(rawPayload: String, eventType: EventType): ReservationEvent {
        return try {
            val dto = parsePayload(rawPayload)
            val ctx = TranslationContext(eventType, rawPayload)
            mapToEvent(dto, ctx)
        } catch (e: TranslationException) {
            throw e
        } catch (e: Exception) {
            throw TranslationException("${getPlatformType()} payload 변환 실패: ${e.message}", e)
        }
    }

    protected open fun parsePayload(rawPayload: String): T {
        return objectMapper.readValue(rawPayload, getDtoClass())
    }

    protected abstract fun getPlatformType(): PlatformType

    protected abstract fun getDtoClass(): Class<T>

    protected abstract fun mapToEvent(dto: T, ctx: TranslationContext): ReservationEvent
}
