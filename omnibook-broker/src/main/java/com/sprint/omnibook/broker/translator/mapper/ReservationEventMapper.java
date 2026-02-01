package com.sprint.omnibook.broker.translator.mapper;

import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.translator.TranslationContext;
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload;
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload;
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 플랫폼별 DTO → ReservationEvent 변환 Mapper.
 *
 * @Context TranslationContext로 전달되는 필드:
 * - eventType, rawPayload
 *
 * expression으로 설정되는 필드:
 * - eventId (UUID.randomUUID())
 * - receivedAt (Instant.now())
 */
@Mapper(componentModel = "spring")
public interface ReservationEventMapper {

    // === Yanolja ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.eventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.rawPayload())")
    @Mapping(target = "platformType", constant = "YANOLJA")
    @Mapping(source = "payload.reservationId", target = "platformReservationId")
    @Mapping(source = "payload.roomId", target = "roomId")
    @Mapping(source = "payload.accommodationName", target = "propertyName")
    @Mapping(source = "payload.accommodationAddress", target = "propertyAddress")
    @Mapping(source = "payload.checkInDate", target = "checkIn", qualifiedByName = "parseIsoDate")
    @Mapping(source = "payload.checkOutDate", target = "checkOut", qualifiedByName = "parseIsoDate")
    @Mapping(source = "payload.guestName", target = "guestName")
    @Mapping(source = "payload.guestPhone", target = "guestPhone")
    @Mapping(target = "guestEmail", ignore = true)
    @Mapping(source = "payload.totalPrice", target = "totalAmount", qualifiedByName = "intToBigDecimal")
    @Mapping(source = "payload.status", target = "status", qualifiedByName = "mapYanoljaStatus")
    @Mapping(source = "payload.bookedAt", target = "occurredAt", qualifiedByName = "parseKstDateTime")
    ReservationEvent fromYanolja(YanoljaPayload payload, @Context TranslationContext ctx);

    // === Airbnb ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.eventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.rawPayload())")
    @Mapping(target = "platformType", constant = "AIRBNB")
    @Mapping(source = "payload.confirmationCode", target = "platformReservationId")
    @Mapping(source = "payload.listingId", target = "roomId")
    @Mapping(source = "payload.listingName", target = "propertyName")
    @Mapping(source = "payload.listingAddress", target = "propertyAddress")
    @Mapping(source = "payload.checkIn", target = "checkIn", qualifiedByName = "parseIsoDate")
    @Mapping(source = "payload.checkOut", target = "checkOut", qualifiedByName = "parseIsoDate")
    @Mapping(source = "payload", target = "guestName", qualifiedByName = "combineAirbnbGuestName")
    @Mapping(target = "guestPhone", ignore = true)
    @Mapping(source = "payload.guestEmail", target = "guestEmail")
    @Mapping(source = "payload.totalPayout", target = "totalAmount", qualifiedByName = "doubleToBigDecimal")
    @Mapping(source = "payload.status", target = "status", qualifiedByName = "mapAirbnbStatus")
    @Mapping(source = "payload.createdAt", target = "occurredAt", qualifiedByName = "parseEpochMillis")
    ReservationEvent fromAirbnb(AirbnbPayload payload, @Context TranslationContext ctx);

    // === YeogiEottae ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.eventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.rawPayload())")
    @Mapping(target = "platformType", constant = "YEOGIEOTTAE")
    @Mapping(source = "payload.orderId", target = "platformReservationId")
    @Mapping(source = "payload.roomTypeId", target = "roomId")
    @Mapping(source = "payload.accommodationName", target = "propertyName")
    @Mapping(source = "payload.accommodationAddress", target = "propertyAddress")
    @Mapping(source = "payload.startDate", target = "checkIn", qualifiedByName = "parseCompactDate")
    @Mapping(source = "payload.endDate", target = "checkOut", qualifiedByName = "parseCompactDate")
    @Mapping(source = "payload.buyerName", target = "guestName")
    @Mapping(source = "payload.buyerTel", target = "guestPhone")
    @Mapping(target = "guestEmail", ignore = true)
    @Mapping(source = "payload.totalAmount", target = "totalAmount", qualifiedByName = "intToBigDecimal")
    @Mapping(source = "payload.state", target = "status", qualifiedByName = "mapYeogieottaeStatus")
    @Mapping(source = "payload.registeredTs", target = "occurredAt", qualifiedByName = "parseEpochSeconds")
    ReservationEvent fromYeogieottae(YeogieottaePayload payload, @Context TranslationContext ctx);

    // === 커스텀 매핑 메서드 ===

    @Named("parseIsoDate")
    default LocalDate parseIsoDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Named("parseCompactDate")
    default LocalDate parseCompactDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Named("parseKstDateTime")
    default Instant parseKstDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) return Instant.now();
        LocalDateTime ldt = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }

    @Named("parseEpochMillis")
    default Instant parseEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    @Named("parseEpochSeconds")
    default Instant parseEpochSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds);
    }

    @Named("intToBigDecimal")
    default BigDecimal intToBigDecimal(int value) {
        return BigDecimal.valueOf(value);
    }

    @Named("doubleToBigDecimal")
    default BigDecimal doubleToBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    @Named("combineAirbnbGuestName")
    default String combineAirbnbGuestName(AirbnbPayload payload) {
        String firstName = payload.getGuestFirstName();
        String lastName = payload.getGuestLastName();
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    @Named("mapYanoljaStatus")
    default ReservationStatus mapYanoljaStatus(String status) {
        if (status == null) return ReservationStatus.PENDING;
        return switch (status) {
            case "예약완료" -> ReservationStatus.CONFIRMED;
            case "취소" -> ReservationStatus.CANCELLED;
            case "노쇼" -> ReservationStatus.NOSHOW;
            default -> ReservationStatus.PENDING;
        };
    }

    @Named("mapAirbnbStatus")
    default ReservationStatus mapAirbnbStatus(String status) {
        if (status == null) return ReservationStatus.PENDING;
        return switch (status) {
            case "ACCEPTED" -> ReservationStatus.CONFIRMED;
            case "PENDING" -> ReservationStatus.PENDING;
            case "CANCELLED", "DENIED" -> ReservationStatus.CANCELLED;
            default -> ReservationStatus.PENDING;
        };
    }

    @Named("mapYeogieottaeStatus")
    default ReservationStatus mapYeogieottaeStatus(int state) {
        return switch (state) {
            case 1 -> ReservationStatus.CONFIRMED;
            case 2 -> ReservationStatus.CANCELLED;
            case 3 -> ReservationStatus.COMPLETED;
            case 4 -> ReservationStatus.NOSHOW;
            default -> ReservationStatus.PENDING;
        };
    }
}
