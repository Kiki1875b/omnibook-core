package com.sprint.omnibook.broker.translator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

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
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirbnbPayload {

    private String confirmationCode;
    private String listingId;
    private String hostId;

    private String checkIn;
    private String checkOut;
    private int nights;

    private String guestFirstName;
    private String guestLastName;
    private String guestEmail;
    private int numberOfGuests;
    private int numberOfAdults;
    private int numberOfChildren;
    private int numberOfInfants;

    private double totalPayout;
    private double hostServiceFee;
    private double guestServiceFee;
    private double cleaningFee;
    private String currency;

    private String status;
    private String cancellationPolicy;

    private String timezone;
    private long createdAt;
    private long updatedAt;
}
