package com.sprint.omnibook.broker.event

/**
 * 예약 상태.
 * 각 플랫폼의 상태값을 정규화한 결과.
 */
enum class ReservationStatus {
    CONFIRMED,
    CANCELLED,
    NOSHOW,
    PENDING,
    COMPLETED
}
