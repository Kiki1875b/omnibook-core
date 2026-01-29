package com.sprint.omnibook.broker.translator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

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
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YanoljaPayload {

    private String reservationId;
    private String roomId;
    private String roomName;
    private String accommodationName;

    private String checkInDate;
    private String checkOutDate;
    private int stayNights;

    private String guestName;
    private String guestPhone;

    private String couponCode;
    private int discountAmount;

    private int totalPrice;
    private String paymentMethod;

    private String status;
    private String bookedAt;
    private String platform;
}
