package com.sprint.omnibook.broker.translator.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Yanolja 플랫폼 payload DTO.
 *
 * 특성:
 * - reservationId: YNJ-xxxxxxxx
 * - 날짜: yyyy-MM-dd
 * - 금액: KRW 정수
 * - 상태: 한글 (예약완료, 취소, 노쇼)
 * - 시간: yyyy-MM-dd'T'HH:mm:ss (KST)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class YanoljaPayload(
    var reservationId: String? = null,
    var roomId: String? = null,
    var roomName: String? = null,
    var accommodationName: String? = null,
    var accommodationAddress: String? = null,

    var checkInDate: String? = null,
    var checkOutDate: String? = null,
    var stayNights: Int = 0,

    var guestName: String? = null,
    var guestPhone: String? = null,

    var couponCode: String? = null,
    var discountAmount: Int = 0,

    var totalPrice: Int = 0,
    var paymentMethod: String? = null,

    var status: String? = null,
    var bookedAt: String? = null,
    var platform: String? = null
)
