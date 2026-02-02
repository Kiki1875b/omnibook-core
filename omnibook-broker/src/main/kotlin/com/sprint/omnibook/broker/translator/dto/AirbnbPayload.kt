package com.sprint.omnibook.broker.translator.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Airbnb 플랫폼 payload DTO.
 *
 * 특성:
 * - confirmationCode: ABC123XYZW
 * - 날짜: yyyy-MM-dd (ISO-8601)
 * - 금액: double, 다양한 currency (현재는 KRW 가정)
 * - 상태: 영문 (ACCEPTED, PENDING, CANCELLED, DENIED)
 * - 시간: epoch millis
 * - 게스트 이름: firstName + lastName 분리
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AirbnbPayload(
    var confirmationCode: String? = null,
    var listingId: String? = null,
    var listingName: String? = null,
    var listingAddress: String? = null,

    var checkIn: String? = null,
    var checkOut: String? = null,
    var nights: Int = 0,

    var guestFirstName: String? = null,
    var guestLastName: String? = null,
    var guestEmail: String? = null,
    var numberOfGuests: Int = 0,
    var numberOfAdults: Int = 0,
    var numberOfChildren: Int = 0,
    var numberOfInfants: Int = 0,

    var totalPayout: Double = 0.0,
    var hostServiceFee: Double = 0.0,
    var guestServiceFee: Double = 0.0,
    var cleaningFee: Double = 0.0,
    var currency: String? = null,

    var status: String? = null,
    var cancellationPolicy: String? = null,

    var timezone: String? = null,
    var createdAt: Long = 0,
    var updatedAt: Long = 0
)
