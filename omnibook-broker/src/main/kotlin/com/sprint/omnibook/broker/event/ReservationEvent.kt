package com.sprint.omnibook.broker.event

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 정규화된 예약 이벤트.
 *
 * 각 OTA 플랫폼(Yanolja, Airbnb, YeogiEottae)의 서로 다른 payload를
 * 브로커 내부에서 일관되게 처리하기 위한 통합 모델.
 *
 * 불변 객체로 설계 — 생성 후 변경 불가.
 */
data class ReservationEvent(
    // === 식별자 ===

    /** 브로커 내부 이벤트 ID (UUID, 수신 시 생성) */
    val eventId: UUID,

    /** 원본 플랫폼 유형 */
    val platformType: PlatformType,

    /** 원본 플랫폼의 예약 ID (중복 판단 기준) */
    val platformReservationId: String? = null,

    /** 이벤트 유형 */
    val eventType: EventType? = null,

    // === 숙소/객실 정보 ===

    /** 객실 ID (정규화) */
    val roomId: String? = null,

    /** 숙소 이름 (감사용) */
    val propertyName: String? = null,

    /** 숙소 주소 */
    val propertyAddress: String? = null,

    // === 일정 ===

    /** 체크인 날짜 */
    val checkIn: LocalDate? = null,

    /** 체크아웃 날짜 */
    val checkOut: LocalDate? = null,

    // === 게스트 정보 ===

    /** 게스트 이름 */
    val guestName: String? = null,

    /** 게스트 전화번호 (nullable) */
    val guestPhone: String? = null,

    /** 게스트 이메일 (nullable) */
    val guestEmail: String? = null,

    // === 금액 ===

    /** 총 금액 (KRW) */
    val totalAmount: BigDecimal? = null,

    // === 상태 ===

    /** 예약 상태 (정규화) */
    val status: ReservationStatus,

    // === 시간 ===

    /** 이벤트 발생 시각 (원본 플랫폼 기준) */
    val occurredAt: Instant? = null,

    /** 브로커 수신 시각 */
    val receivedAt: Instant,

    // === 원본 보존 ===

    /** 원본 JSON payload (디버깅/감사용) */
    val rawPayload: String? = null
)
