package com.sprint.omnibook.broker.translator;

import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.ReservationEvent;

/**
 * 플랫폼별 payload를 ReservationEvent로 변환하는 인터페이스.
 *
 * 각 플랫폼(Yanolja, Airbnb, YeogiEottae)마다 구현체가 존재한다.
 */
public interface PayloadTranslator {

    /**
     * 원본 JSON payload를 정규화된 ReservationEvent로 변환한다.
     *
     * @param rawPayload 원본 JSON 문자열
     * @param eventType  이벤트 유형 (BOOKING, CANCELLATION 등)
     * @return 정규화된 ReservationEvent
     * @throws TranslationException 파싱 또는 변환 실패 시
     */
    ReservationEvent translate(String rawPayload, EventType eventType);
}
