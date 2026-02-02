package com.sprint.omnibook.broker.translator.mapper

import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.event.ReservationStatus
import com.sprint.omnibook.broker.translator.TranslationContext
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload
import com.sprint.omnibook.broker.translator.dto.AirbnbStatusCode
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload
import com.sprint.omnibook.broker.translator.dto.YanoljaStatusCode
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload
import com.sprint.omnibook.broker.translator.dto.YeogieottaeStatusCode
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
abstract class ReservationEventMapper {

    // === Yanolja ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.getEventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.getRawPayload())")
    @Mapping(target = "platformType", constant = "YANOLJA")
    @Mapping(source = "payload.reservationId", target = "platformReservationId")
    @Mapping(source = "payload.roomId", target = "roomId")
    @Mapping(source = "payload.accommodationName", target = "propertyName")
    @Mapping(source = "payload.accommodationAddress", target = "propertyAddress")
    @Mapping(source = "payload.checkInDate", target = "checkIn", qualifiedByName = ["parseIsoDate"])
    @Mapping(source = "payload.checkOutDate", target = "checkOut", qualifiedByName = ["parseIsoDate"])
    @Mapping(source = "payload.guestName", target = "guestName")
    @Mapping(source = "payload.guestPhone", target = "guestPhone")
    @Mapping(target = "guestEmail", ignore = true)
    @Mapping(source = "payload.totalPrice", target = "totalAmount", qualifiedByName = ["intToBigDecimal"])
    @Mapping(source = "payload.status", target = "status", qualifiedByName = ["mapYanoljaStatus"])
    @Mapping(source = "payload.bookedAt", target = "occurredAt", qualifiedByName = ["parseKstDateTime"])
    abstract fun fromYanolja(payload: YanoljaPayload, @Context ctx: TranslationContext): ReservationEvent

    // === Airbnb ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.getEventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.getRawPayload())")
    @Mapping(target = "platformType", constant = "AIRBNB")
    @Mapping(source = "payload.confirmationCode", target = "platformReservationId")
    @Mapping(source = "payload.listingId", target = "roomId")
    @Mapping(source = "payload.listingName", target = "propertyName")
    @Mapping(source = "payload.listingAddress", target = "propertyAddress")
    @Mapping(source = "payload.checkIn", target = "checkIn", qualifiedByName = ["parseIsoDate"])
    @Mapping(source = "payload.checkOut", target = "checkOut", qualifiedByName = ["parseIsoDate"])
    @Mapping(source = "payload", target = "guestName", qualifiedByName = ["combineAirbnbGuestName"])
    @Mapping(target = "guestPhone", ignore = true)
    @Mapping(source = "payload.guestEmail", target = "guestEmail")
    @Mapping(source = "payload.totalPayout", target = "totalAmount", qualifiedByName = ["doubleToBigDecimal"])
    @Mapping(source = "payload.status", target = "status", qualifiedByName = ["mapAirbnbStatus"])
    @Mapping(source = "payload.createdAt", target = "occurredAt", qualifiedByName = ["parseEpochMillis"])
    abstract fun fromAirbnb(payload: AirbnbPayload, @Context ctx: TranslationContext): ReservationEvent

    // === YeogiEottae ===

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "eventType", expression = "java(ctx.getEventType())")
    @Mapping(target = "receivedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "rawPayload", expression = "java(ctx.getRawPayload())")
    @Mapping(target = "platformType", constant = "YEOGIEOTTAE")
    @Mapping(source = "payload.orderId", target = "platformReservationId")
    @Mapping(source = "payload.roomTypeId", target = "roomId")
    @Mapping(source = "payload.accommodationName", target = "propertyName")
    @Mapping(source = "payload.accommodationAddress", target = "propertyAddress")
    @Mapping(source = "payload.startDate", target = "checkIn", qualifiedByName = ["parseCompactDate"])
    @Mapping(source = "payload.endDate", target = "checkOut", qualifiedByName = ["parseCompactDate"])
    @Mapping(source = "payload.buyerName", target = "guestName")
    @Mapping(source = "payload.buyerTel", target = "guestPhone")
    @Mapping(target = "guestEmail", ignore = true)
    @Mapping(source = "payload.totalAmount", target = "totalAmount", qualifiedByName = ["intToBigDecimal"])
    @Mapping(source = "payload.state", target = "status", qualifiedByName = ["mapYeogieottaeStatus"])
    @Mapping(source = "payload.registeredTs", target = "occurredAt", qualifiedByName = ["parseEpochSeconds"])
    abstract fun fromYeogieottae(payload: YeogieottaePayload, @Context ctx: TranslationContext): ReservationEvent

    // === 커스텀 매핑 메서드 ===

    @Named("parseIsoDate")
    fun parseIsoDate(date: String?): LocalDate? {
        if (date.isNullOrBlank()) return null
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @Named("parseCompactDate")
    fun parseCompactDate(date: String?): LocalDate? {
        if (date.isNullOrBlank()) return null
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(DateTimePatterns.COMPACT_DATE))
    }

    @Named("parseKstDateTime")
    fun parseKstDateTime(dateTime: String?): Instant {
        if (dateTime.isNullOrBlank()) return Instant.now()
        val ldt = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern(DateTimePatterns.KST_DATETIME))
        return ldt.atZone(ZoneId.of(DateTimePatterns.TIMEZONE_KST)).toInstant()
    }

    @Named("parseEpochMillis")
    fun parseEpochMillis(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)

    @Named("parseEpochSeconds")
    fun parseEpochSeconds(epochSeconds: Long): Instant = Instant.ofEpochSecond(epochSeconds)

    @Named("intToBigDecimal")
    fun intToBigDecimal(value: Int): BigDecimal = BigDecimal.valueOf(value.toLong())

    @Named("doubleToBigDecimal")
    fun doubleToBigDecimal(value: Double): BigDecimal = BigDecimal.valueOf(value)

    @Named("combineAirbnbGuestName")
    fun combineAirbnbGuestName(payload: AirbnbPayload): String {
        val firstName = payload.guestFirstName ?: ""
        val lastName = payload.guestLastName ?: ""
        return "$firstName $lastName".trim()
    }

    @Named("mapYanoljaStatus")
    fun mapYanoljaStatus(status: String?): ReservationStatus {
        if (status == null) return ReservationStatus.PENDING
        return when (status) {
            YanoljaStatusCode.CONFIRMED -> ReservationStatus.CONFIRMED
            YanoljaStatusCode.CANCELLED -> ReservationStatus.CANCELLED
            YanoljaStatusCode.NOSHOW -> ReservationStatus.NOSHOW
            else -> ReservationStatus.PENDING
        }
    }

    @Named("mapAirbnbStatus")
    fun mapAirbnbStatus(status: String?): ReservationStatus {
        if (status == null) return ReservationStatus.PENDING
        return when (status) {
            AirbnbStatusCode.ACCEPTED -> ReservationStatus.CONFIRMED
            AirbnbStatusCode.PENDING -> ReservationStatus.PENDING
            AirbnbStatusCode.CANCELLED, AirbnbStatusCode.DENIED -> ReservationStatus.CANCELLED
            else -> ReservationStatus.PENDING
        }
    }

    @Named("mapYeogieottaeStatus")
    fun mapYeogieottaeStatus(state: Int): ReservationStatus {
        return when (state) {
            YeogieottaeStatusCode.CONFIRMED -> ReservationStatus.CONFIRMED
            YeogieottaeStatusCode.CANCELLED -> ReservationStatus.CANCELLED
            YeogieottaeStatusCode.COMPLETED -> ReservationStatus.COMPLETED
            YeogieottaeStatusCode.NOSHOW -> ReservationStatus.NOSHOW
            else -> ReservationStatus.PENDING
        }
    }
}
