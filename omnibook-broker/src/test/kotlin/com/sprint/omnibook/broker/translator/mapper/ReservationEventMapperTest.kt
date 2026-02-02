package com.sprint.omnibook.broker.translator.mapper

import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationStatus
import com.sprint.omnibook.broker.translator.TranslationContext
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@DisplayName("ReservationEventMapper")
class ReservationEventMapperTest {

    private lateinit var mapper: ReservationEventMapper

    @BeforeEach
    fun setUp() {
        mapper = ReservationEventMapperImpl()
    }

    @Nested
    @DisplayName("fromYanolja 메서드는")
    inner class Describe_fromYanolja {

        @Test
        @DisplayName("YanoljaPayload를 ReservationEvent로 매핑한다")
        fun it_maps_yanolja_payload() {
            // given
            val payload = YanoljaPayload().apply {
                reservationId = "YNJ-12345678"
                roomId = "R-101"
                accommodationName = "서울 호텔"
                checkInDate = "2025-08-15"
                checkOutDate = "2025-08-18"
                guestName = "김민수"
                guestPhone = "010-1234-5678"
                totalPrice = 450000
                status = "예약완료"
                bookedAt = "2025-08-01T10:30:00"
            }

            val ctx = TranslationContext(EventType.BOOKING, "raw json")

            // when
            val result = mapper.fromYanolja(payload, ctx)

            // then
            assertThat(result.eventId).isNotNull
            assertThat(result.platformType).isEqualTo(PlatformType.YANOLJA)
            assertThat(result.platformReservationId).isEqualTo("YNJ-12345678")
            assertThat(result.eventType).isEqualTo(EventType.BOOKING)
            assertThat(result.roomId).isEqualTo("R-101")
            assertThat(result.propertyName).isEqualTo("서울 호텔")
            assertThat(result.checkIn).isEqualTo(LocalDate.of(2025, 8, 15))
            assertThat(result.checkOut).isEqualTo(LocalDate.of(2025, 8, 18))
            assertThat(result.guestName).isEqualTo("김민수")
            assertThat(result.guestPhone).isEqualTo("010-1234-5678")
            assertThat(result.guestEmail).isNull()
            assertThat(result.totalAmount).isEqualTo(BigDecimal.valueOf(450000))
            assertThat(result.status).isEqualTo(ReservationStatus.CONFIRMED)
            assertThat(result.occurredAt).isNotNull
            assertThat(result.receivedAt).isNotNull
            assertThat(result.rawPayload).isEqualTo("raw json")
        }

        @ParameterizedTest
        @CsvSource(
            "예약완료, CONFIRMED",
            "취소, CANCELLED",
            "노쇼, NOSHOW",
            "알수없음, PENDING"
        )
        @DisplayName("한글 상태를 ReservationStatus로 매핑한다")
        fun it_maps_korean_status(koreanStatus: String, expected: ReservationStatus) {
            // given
            val payload = createMinimalYanoljaPayload().apply {
                status = koreanStatus
            }
            val ctx = TranslationContext(EventType.BOOKING, "")

            // when
            val result = mapper.fromYanolja(payload, ctx)

            // then
            assertThat(result.status).isEqualTo(expected)
        }

        private fun createMinimalYanoljaPayload(): YanoljaPayload {
            return YanoljaPayload().apply {
                reservationId = "YNJ-TEST"
                roomId = "R-001"
                checkInDate = "2025-01-01"
                checkOutDate = "2025-01-02"
                guestName = "테스트"
                totalPrice = 100000
            }
        }
    }

    @Nested
    @DisplayName("fromAirbnb 메서드는")
    inner class Describe_fromAirbnb {

        @Test
        @DisplayName("AirbnbPayload를 ReservationEvent로 매핑한다")
        fun it_maps_airbnb_payload() {
            // given
            val createdAt = 1723456789000L
            val payload = AirbnbPayload().apply {
                confirmationCode = "ABC123XYZW"
                listingId = "listing-001"
                listingName = "Seoul Apartment"
                checkIn = "2025-08-15"
                checkOut = "2025-08-18"
                guestFirstName = "Minsu"
                guestLastName = "Kim"
                guestEmail = "minsu@example.com"
                totalPayout = 350.50
                status = "ACCEPTED"
                this.createdAt = createdAt
            }

            val ctx = TranslationContext(EventType.BOOKING, "raw json")

            // when
            val result = mapper.fromAirbnb(payload, ctx)

            // then
            assertThat(result.platformType).isEqualTo(PlatformType.AIRBNB)
            assertThat(result.platformReservationId).isEqualTo("ABC123XYZW")
            assertThat(result.roomId).isEqualTo("listing-001")
            assertThat(result.propertyName).isEqualTo("Seoul Apartment")
            assertThat(result.guestName).isEqualTo("Minsu Kim")
            assertThat(result.guestPhone).isNull()
            assertThat(result.guestEmail).isEqualTo("minsu@example.com")
            assertThat(result.totalAmount).isEqualTo(BigDecimal.valueOf(350.5))
            assertThat(result.status).isEqualTo(ReservationStatus.CONFIRMED)
            assertThat(result.occurredAt).isEqualTo(Instant.ofEpochMilli(createdAt))
        }

        @Test
        @DisplayName("firstName만 있으면 lastName 없이 이름을 조합한다")
        fun it_combines_guest_name_with_first_name_only() {
            // given
            val payload = createMinimalAirbnbPayload().apply {
                guestFirstName = "SingleName"
                guestLastName = null
            }
            val ctx = TranslationContext(EventType.BOOKING, "")

            // when
            val result = mapper.fromAirbnb(payload, ctx)

            // then
            assertThat(result.guestName).isEqualTo("SingleName")
        }

        @ParameterizedTest
        @CsvSource(
            "ACCEPTED, CONFIRMED",
            "PENDING, PENDING",
            "CANCELLED, CANCELLED",
            "DENIED, CANCELLED",
            "UNKNOWN, PENDING"
        )
        @DisplayName("영문 상태를 ReservationStatus로 매핑한다")
        fun it_maps_english_status(englishStatus: String, expected: ReservationStatus) {
            // given
            val payload = createMinimalAirbnbPayload().apply {
                status = englishStatus
            }
            val ctx = TranslationContext(EventType.BOOKING, "")

            // when
            val result = mapper.fromAirbnb(payload, ctx)

            // then
            assertThat(result.status).isEqualTo(expected)
        }

        private fun createMinimalAirbnbPayload(): AirbnbPayload {
            return AirbnbPayload().apply {
                confirmationCode = "TEST001"
                listingId = "listing-001"
                checkIn = "2025-01-01"
                checkOut = "2025-01-02"
                totalPayout = 100.0
                status = "ACCEPTED"
                createdAt = 1723456789000L
            }
        }
    }

    @Nested
    @DisplayName("fromYeogieottae 메서드는")
    inner class Describe_fromYeogieottae {

        @Test
        @DisplayName("YeogieottaePayload를 ReservationEvent로 매핑한다")
        fun it_maps_yeogieottae_payload() {
            // given
            val registeredTs = 1723456789L
            val payload = YeogieottaePayload().apply {
                orderId = "YEO-12345678"
                accommodationId = "ACC-001"
                accommodationName = "제주 리조트"
                roomTypeId = "RT-101"
                startDate = "20250815"
                endDate = "20250818"
                buyerName = "김민수"
                buyerTel = "01012345678"
                totalAmount = 380000
                state = 1
                this.registeredTs = registeredTs
            }

            val ctx = TranslationContext(EventType.BOOKING, "raw json")

            // when
            val result = mapper.fromYeogieottae(payload, ctx)

            // then
            assertThat(result.platformType).isEqualTo(PlatformType.YEOGIEOTTAE)
            assertThat(result.platformReservationId).isEqualTo("YEO-12345678")
            assertThat(result.roomId).isEqualTo("RT-101")
            assertThat(result.propertyName).isEqualTo("제주 리조트")
            assertThat(result.checkIn).isEqualTo(LocalDate.of(2025, 8, 15))
            assertThat(result.checkOut).isEqualTo(LocalDate.of(2025, 8, 18))
            assertThat(result.guestName).isEqualTo("김민수")
            assertThat(result.guestPhone).isEqualTo("01012345678")
            assertThat(result.guestEmail).isNull()
            assertThat(result.totalAmount).isEqualTo(BigDecimal.valueOf(380000))
            assertThat(result.status).isEqualTo(ReservationStatus.CONFIRMED)
            assertThat(result.occurredAt).isEqualTo(Instant.ofEpochSecond(registeredTs))
        }

        @ParameterizedTest
        @CsvSource(
            "1, CONFIRMED",
            "2, CANCELLED",
            "3, COMPLETED",
            "4, NOSHOW",
            "99, PENDING"
        )
        @DisplayName("숫자 상태를 ReservationStatus로 매핑한다")
        fun it_maps_numeric_status(state: Int, expected: ReservationStatus) {
            // given
            val payload = createMinimalYeogieottaePayload().apply {
                this.state = state
            }
            val ctx = TranslationContext(EventType.BOOKING, "")

            // when
            val result = mapper.fromYeogieottae(payload, ctx)

            // then
            assertThat(result.status).isEqualTo(expected)
        }

        @Test
        @DisplayName("compact 날짜 형식(yyyyMMdd)을 LocalDate로 파싱한다")
        fun it_parses_compact_date_format() {
            // given
            val payload = createMinimalYeogieottaePayload().apply {
                startDate = "20251225"
                endDate = "20251231"
            }
            val ctx = TranslationContext(EventType.BOOKING, "")

            // when
            val result = mapper.fromYeogieottae(payload, ctx)

            // then
            assertThat(result.checkIn).isEqualTo(LocalDate.of(2025, 12, 25))
            assertThat(result.checkOut).isEqualTo(LocalDate.of(2025, 12, 31))
        }

        private fun createMinimalYeogieottaePayload(): YeogieottaePayload {
            return YeogieottaePayload().apply {
                orderId = "YEO-TEST"
                roomTypeId = "RT-001"
                accommodationId = "ACC-001"
                startDate = "20250101"
                endDate = "20250102"
                buyerName = "테스트"
                totalAmount = 100000
                state = 1
                registeredTs = 1723456789L
            }
        }
    }
}
