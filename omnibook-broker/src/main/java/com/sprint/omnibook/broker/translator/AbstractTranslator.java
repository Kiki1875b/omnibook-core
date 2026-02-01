package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public abstract class AbstractTranslator<T> implements PayloadTranslator {

    protected final ObjectMapper objectMapper;

    @Override
    public final ReservationEvent translate(String rawPayload, EventType eventType) {
        try {
            T dto = parsePayload(rawPayload);
            TranslationContext ctx = new TranslationContext(eventType, rawPayload);
            return mapToEvent(dto, ctx);
        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            throw new TranslationException(getPlatformType() + " payload 변환 실패: " + e.getMessage(), e);
        }
    }

    protected T parsePayload(String rawPayload) throws Exception {
        return objectMapper.readValue(rawPayload, getDtoClass());
    }

    protected abstract PlatformType getPlatformType();

    protected abstract Class<T> getDtoClass();

    protected abstract ReservationEvent mapToEvent(T dto, TranslationContext ctx);
}
