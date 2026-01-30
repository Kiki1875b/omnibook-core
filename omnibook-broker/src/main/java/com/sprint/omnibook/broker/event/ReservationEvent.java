package com.sprint.omnibook.broker.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 정규화된 예약 이벤트.
 *
 * 각 OTA 플랫폼(Yanolja, Airbnb, YeogiEottae)의 서로 다른 payload를
 * 브로커 내부에서 일관되게 처리하기 위한 통합 모델.
 *
 * 불변 객체로 설계 — 생성 후 변경 불가.
 */
@Getter
@Builder
public class ReservationEvent {

    // === 식별자 ===

    /** 브로커 내부 이벤트 ID (UUID, 수신 시 생성) */
    private final UUID eventId;

    /** 원본 플랫폼 유형 */
    private final PlatformType platformType;

    /** 원본 플랫폼의 예약 ID (중복 판단 기준) */
    private final String platformReservationId;

    /** 이벤트 유형 */
    private final EventType eventType;

    // === 숙소/객실 정보 ===

    /** 객실 ID (정규화) */
    private final String roomId;

    /** 숙소 ID (정규화) */
    private final String propertyId;

    /** 숙소 이름 */
    private final String propertyName;

    /** 숙소 주소 */
    private final String propertyAddress;

    // === 일정 ===

    /** 체크인 날짜 */
    private final LocalDate checkIn;

    /** 체크아웃 날짜 */
    private final LocalDate checkOut;

    // === 게스트 정보 ===

    /** 게스트 이름 */
    private final String guestName;

    /** 게스트 전화번호 (nullable) */
    private final String guestPhone;

    /** 게스트 이메일 (nullable) */
    private final String guestEmail;

    // === 금액 ===

    /** 총 금액 (KRW) */
    private final BigDecimal totalAmount;

    // === 상태 ===

    /** 예약 상태 (정규화) */
    private final ReservationStatus status;

    // === 시간 ===

    /** 이벤트 발생 시각 (원본 플랫폼 기준) */
    private final Instant occurredAt;

    /** 브로커 수신 시각 */
    private final Instant receivedAt;

    // === 원본 보존 ===

    /** 원본 JSON payload (디버깅/감사용) */
    private final String rawPayload;
}
