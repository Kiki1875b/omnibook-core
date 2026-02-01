package com.sprint.omnibook.broker.translator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

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
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YeogieottaePayload {

    private String orderId;
    private String accommodationId;
    private String accommodationName;
    private String accommodationAddress;
    private String roomTypeId;
    private String roomTypeName;

    private String startDate;
    private String endDate;

    private String buyerName;
    private String buyerTel;

    private int totalAmount;
    private String payMethod;

    private int state;
    private long registeredTs;
    private long lastModifiedTs;
}
