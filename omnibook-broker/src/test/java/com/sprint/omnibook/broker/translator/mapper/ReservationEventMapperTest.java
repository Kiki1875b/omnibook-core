package com.sprint.omnibook.broker.translator.mapper;

import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.translator.TranslationContext;
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload;
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload;
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservationEventMapper")
class ReservationEventMapperTest {

    private ReservationEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservationEventMapperImpl();
    }

    @Nested
    @DisplayName("fromYanolja 메서드는")
    class Describe_fromYanolja {

        @Test
        @DisplayName("YanoljaPayload를 ReservationEvent로 매핑한다")
        void it_maps_yanolja_payload() {
            // given
            YanoljaPayload payload = new YanoljaPayload();
            payload.setReservationId("YNJ-12345678");
            payload.setRoomId("R-101");
            payload.setAccommodationName("서울 호텔");
            payload.setCheckInDate("2025-08-15");
            payload.setCheckOutDate("2025-08-18");
            payload.setGuestName("김민수");
            payload.setGuestPhone("010-1234-5678");
            payload.setTotalPrice(450000);
            payload.setStatus("예약완료");
            payload.setBookedAt("2025-08-01T10:30:00");

            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "raw json");

            // when
            ReservationEvent result = mapper.fromYanolja(payload, ctx);

            // then
            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getPlatformType()).isEqualTo(PlatformType.YANOLJA);
            assertThat(result.getPlatformReservationId()).isEqualTo("YNJ-12345678");
            assertThat(result.getEventType()).isEqualTo(EventType.BOOKING);
            assertThat(result.getRoomId()).isEqualTo("R-101");
            assertThat(result.getPropertyName()).isEqualTo("서울 호텔");
            assertThat(result.getCheckIn()).isEqualTo(LocalDate.of(2025, 8, 15));
            assertThat(result.getCheckOut()).isEqualTo(LocalDate.of(2025, 8, 18));
            assertThat(result.getGuestName()).isEqualTo("김민수");
            assertThat(result.getGuestPhone()).isEqualTo("010-1234-5678");
            assertThat(result.getGuestEmail()).isNull();
            assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(450000));
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result.getOccurredAt()).isNotNull();
            assertThat(result.getReceivedAt()).isNotNull();
            assertThat(result.getRawPayload()).isEqualTo("raw json");
        }

        @ParameterizedTest
        @CsvSource({
                "예약완료, CONFIRMED",
                "취소, CANCELLED",
                "노쇼, NOSHOW",
                "알수없음, PENDING"
        })
        @DisplayName("한글 상태를 ReservationStatus로 매핑한다")
        void it_maps_korean_status(String koreanStatus, ReservationStatus expected) {
            // given
            YanoljaPayload payload = createMinimalYanoljaPayload();
            payload.setStatus(koreanStatus);
            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "");

            // when
            ReservationEvent result = mapper.fromYanolja(payload, ctx);

            // then
            assertThat(result.getStatus()).isEqualTo(expected);
        }

        private YanoljaPayload createMinimalYanoljaPayload() {
            YanoljaPayload payload = new YanoljaPayload();
            payload.setReservationId("YNJ-TEST");
            payload.setRoomId("R-001");
            payload.setCheckInDate("2025-01-01");
            payload.setCheckOutDate("2025-01-02");
            payload.setGuestName("테스트");
            payload.setTotalPrice(100000);
            return payload;
        }
    }

    @Nested
    @DisplayName("fromAirbnb 메서드는")
    class Describe_fromAirbnb {

        @Test
        @DisplayName("AirbnbPayload를 ReservationEvent로 매핑한다")
        void it_maps_airbnb_payload() {
            // given
            long createdAt = 1723456789000L;
            AirbnbPayload payload = new AirbnbPayload();
            payload.setConfirmationCode("ABC123XYZW");
            payload.setListingId("listing-001");
            payload.setHostId("host-999");
            payload.setListingName("Seoul Apartment");
            payload.setCheckIn("2025-08-15");
            payload.setCheckOut("2025-08-18");
            payload.setGuestFirstName("Minsu");
            payload.setGuestLastName("Kim");
            payload.setGuestEmail("minsu@example.com");
            payload.setTotalPayout(350.50);
            payload.setStatus("ACCEPTED");
            payload.setCreatedAt(createdAt);

            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "raw json");

            // when
            ReservationEvent result = mapper.fromAirbnb(payload, ctx);

            // then
            assertThat(result.getPlatformType()).isEqualTo(PlatformType.AIRBNB);
            assertThat(result.getPlatformReservationId()).isEqualTo("ABC123XYZW");
            assertThat(result.getRoomId()).isEqualTo("listing-001");
            assertThat(result.getPropertyName()).isEqualTo("Seoul Apartment");
            assertThat(result.getGuestName()).isEqualTo("Minsu Kim");
            assertThat(result.getGuestPhone()).isNull();
            assertThat(result.getGuestEmail()).isEqualTo("minsu@example.com");
            assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(350.5));
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result.getOccurredAt()).isEqualTo(Instant.ofEpochMilli(createdAt));
        }

        @Test
        @DisplayName("firstName만 있으면 lastName 없이 이름을 조합한다")
        void it_combines_guest_name_with_first_name_only() {
            // given
            AirbnbPayload payload = createMinimalAirbnbPayload();
            payload.setGuestFirstName("SingleName");
            payload.setGuestLastName(null);
            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "");

            // when
            ReservationEvent result = mapper.fromAirbnb(payload, ctx);

            // then
            assertThat(result.getGuestName()).isEqualTo("SingleName");
        }

        @ParameterizedTest
        @CsvSource({
                "ACCEPTED, CONFIRMED",
                "PENDING, PENDING",
                "CANCELLED, CANCELLED",
                "DENIED, CANCELLED",
                "UNKNOWN, PENDING"
        })
        @DisplayName("영문 상태를 ReservationStatus로 매핑한다")
        void it_maps_english_status(String englishStatus, ReservationStatus expected) {
            // given
            AirbnbPayload payload = createMinimalAirbnbPayload();
            payload.setStatus(englishStatus);
            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "");

            // when
            ReservationEvent result = mapper.fromAirbnb(payload, ctx);

            // then
            assertThat(result.getStatus()).isEqualTo(expected);
        }

        private AirbnbPayload createMinimalAirbnbPayload() {
            AirbnbPayload payload = new AirbnbPayload();
            payload.setConfirmationCode("TEST001");
            payload.setListingId("listing-001");
            payload.setCheckIn("2025-01-01");
            payload.setCheckOut("2025-01-02");
            payload.setTotalPayout(100.0);
            payload.setStatus("ACCEPTED");
            payload.setCreatedAt(1723456789000L);
            return payload;
        }
    }

    @Nested
    @DisplayName("fromYeogieottae 메서드는")
    class Describe_fromYeogieottae {

        @Test
        @DisplayName("YeogieottaePayload를 ReservationEvent로 매핑한다")
        void it_maps_yeogieottae_payload() {
            // given
            long registeredTs = 1723456789L;
            YeogieottaePayload payload = new YeogieottaePayload();
            payload.setOrderId("YEO-12345678");
            payload.setAccommodationId("ACC-001");
            payload.setAccommodationName("제주 리조트");
            payload.setRoomTypeId("RT-101");
            payload.setStartDate("20250815");
            payload.setEndDate("20250818");
            payload.setBuyerName("김민수");
            payload.setBuyerTel("01012345678");
            payload.setTotalAmount(380000);
            payload.setState(1);
            payload.setRegisteredTs(registeredTs);

            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "raw json");

            // when
            ReservationEvent result = mapper.fromYeogieottae(payload, ctx);

            // then
            assertThat(result.getPlatformType()).isEqualTo(PlatformType.YEOGIEOTTAE);
            assertThat(result.getPlatformReservationId()).isEqualTo("YEO-12345678");
            assertThat(result.getRoomId()).isEqualTo("RT-101");
            assertThat(result.getPropertyName()).isEqualTo("제주 리조트");
            assertThat(result.getCheckIn()).isEqualTo(LocalDate.of(2025, 8, 15));
            assertThat(result.getCheckOut()).isEqualTo(LocalDate.of(2025, 8, 18));
            assertThat(result.getGuestName()).isEqualTo("김민수");
            assertThat(result.getGuestPhone()).isEqualTo("01012345678");
            assertThat(result.getGuestEmail()).isNull();
            assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(380000));
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result.getOccurredAt()).isEqualTo(Instant.ofEpochSecond(registeredTs));
        }

        @ParameterizedTest
        @CsvSource({
                "1, CONFIRMED",
                "2, CANCELLED",
                "3, COMPLETED",
                "4, NOSHOW",
                "99, PENDING"
        })
        @DisplayName("숫자 상태를 ReservationStatus로 매핑한다")
        void it_maps_numeric_status(int state, ReservationStatus expected) {
            // given
            YeogieottaePayload payload = createMinimalYeogieottaePayload();
            payload.setState(state);
            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "");

            // when
            ReservationEvent result = mapper.fromYeogieottae(payload, ctx);

            // then
            assertThat(result.getStatus()).isEqualTo(expected);
        }

        @Test
        @DisplayName("compact 날짜 형식(yyyyMMdd)을 LocalDate로 파싱한다")
        void it_parses_compact_date_format() {
            // given
            YeogieottaePayload payload = createMinimalYeogieottaePayload();
            payload.setStartDate("20251225");
            payload.setEndDate("20251231");
            TranslationContext ctx = new TranslationContext(EventType.BOOKING, "");

            // when
            ReservationEvent result = mapper.fromYeogieottae(payload, ctx);

            // then
            assertThat(result.getCheckIn()).isEqualTo(LocalDate.of(2025, 12, 25));
            assertThat(result.getCheckOut()).isEqualTo(LocalDate.of(2025, 12, 31));
        }

        private YeogieottaePayload createMinimalYeogieottaePayload() {
            YeogieottaePayload payload = new YeogieottaePayload();
            payload.setOrderId("YEO-TEST");
            payload.setRoomTypeId("RT-001");
            payload.setAccommodationId("ACC-001");
            payload.setStartDate("20250101");
            payload.setEndDate("20250102");
            payload.setBuyerName("테스트");
            payload.setTotalAmount(100000);
            payload.setState(1);
            payload.setRegisteredTs(1723456789L);
            return payload;
        }
    }
}
