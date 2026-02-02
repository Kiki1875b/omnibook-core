package com.sprint.omnibook.broker.translator.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * YeogiEottae 플랫폼 payload DTO.
 *
 * 특성:
 * - orderId: YEO-xxxxxxxx
 * - 날짜: yyyyMMdd (compact, no dashes)
 * - 금액: KRW 정수
 * - 상태: 숫자 (1=BOOKED, 2=CANCELLED, 3=COMPLETED, 4=NOSHOW)
 * - 시간: epoch seconds (not millis)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class YeogieottaePayload(
    var orderId: String? = null,
    var accommodationId: String? = null,
    var accommodationName: String? = null,
    var accommodationAddress: String? = null,
    var roomTypeId: String? = null,
    var roomTypeName: String? = null,

    var startDate: String? = null,
    var endDate: String? = null,

    var buyerName: String? = null,
    var buyerTel: String? = null,

    var totalAmount: Int = 0,
    var payMethod: String? = null,

    var state: Int = 0,
    var registeredTs: Long = 0,
    var lastModifiedTs: Long = 0
)
