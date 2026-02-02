package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload;
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("YeogieottaeTranslator")
class YeogieottaeTranslatorTest {

    @Mock
    private ReservationEventMapper mapper;

    private ObjectMapper objectMapper;
    private YeogieottaeTranslator translator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        translator = new YeogieottaeTranslator(objectMapper, mapper);
    }

    @Nested
    @DisplayName("translate 메서드는")
    class Describe_translate {

        @Nested
        @DisplayName("유효한 YeogiEottae payload가 주어지면")
        class Context_with_valid_payload {

            private final long registeredTs = 1723456789L;
            private final String validJson = """
                {
                    "orderId": "YEO-12345678",
                    "accommodationId": "ACC-001",
                    "roomTypeId": "RT-101",
                    "startDate": "20250815",
                    "endDate": "20250818",
                    "buyerName": "김민수",
                    "buyerTel": "01012345678",
                    "totalAmount": 380000,
                    "state": 1,
                    "registeredTs": %d
                }
                """.formatted(registeredTs);

            private ReservationEvent mockEvent;

            @BeforeEach
            void setUp() {
                mockEvent = ReservationEvent.builder()
                        .eventId(UUID.randomUUID())
                        .platformType(PlatformType.YEOGIEOTTAE)
                        .platformReservationId("YEO-12345678")
                        .eventType(EventType.BOOKING)
                        .roomId("RT-101")
                        .propertyName("ACC-001")
                        .checkIn(LocalDate.of(2025, 8, 15))
                        .checkOut(LocalDate.of(2025, 8, 18))
                        .guestName("김민수")
                        .guestPhone("01012345678")
                        .totalAmount(BigDecimal.valueOf(380000))
                        .status(ReservationStatus.CONFIRMED)
                        .occurredAt(Instant.ofEpochSecond(registeredTs))
                        .receivedAt(Instant.now())
                        .rawPayload(validJson)
                        .build();

                given(mapper.fromYeogieottae(any(YeogieottaePayload.class), any(TranslationContext.class)))
                        .willReturn(mockEvent);
            }

            @Test
            @DisplayName("ReservationEvent를 반환한다")
            void it_returns_reservation_event() {
                // when
                ReservationEvent result = translator.translate(validJson, EventType.BOOKING);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPlatformType()).isEqualTo(PlatformType.YEOGIEOTTAE);
                assertThat(result.getPlatformReservationId()).isEqualTo("YEO-12345678");
            }

            @Test
            @DisplayName("Mapper의 fromYeogieottae를 호출한다")
            void it_calls_mapper_fromYeogieottae() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromYeogieottae(any(YeogieottaePayload.class), any(TranslationContext.class));
            }

            @Test
            @DisplayName("DTO에 compact 날짜 형식(yyyyMMdd)이 파싱된다")
            void it_parses_compact_date_format() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromYeogieottae(
                        org.mockito.ArgumentMatchers.argThat(payload ->
                                "20250815".equals(payload.getStartDate()) &&
                                "20250818".equals(payload.getEndDate())
                        ),
                        any(TranslationContext.class)
                );
            }

            @Test
            @DisplayName("DTO에 epoch seconds 타임스탬프가 파싱된다")
            void it_parses_epoch_seconds() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromYeogieottae(
                        org.mockito.ArgumentMatchers.argThat(payload ->
                                payload.getRegisteredTs() == registeredTs
                        ),
                        any(TranslationContext.class)
                );
            }
        }

        @Nested
        @DisplayName("잘못된 JSON이 주어지면")
        class Context_with_invalid_json {

            @Test
            @DisplayName("TranslationException을 던진다")
            void it_throws_translation_exception() {
                // given
                String invalidJson = "{ broken json";

                // when & then
                assertThatThrownBy(() -> translator.translate(invalidJson, EventType.BOOKING))
                        .isInstanceOf(TranslationException.class)
                        .hasMessageContaining("YEOGIEOTTAE")
                        .hasMessageContaining("변환 실패");
            }
        }

        @Nested
        @DisplayName("state 필드 값에 따라")
        class Context_with_different_states {

            @Test
            @DisplayName("state=1이면 DTO에 1이 파싱된다")
            void it_parses_state_1() {
                // given
                String json = """
                    {
                        "orderId": "YEO-STATE001",
                        "roomTypeId": "RT-101",
                        "accommodationId": "ACC-001",
                        "startDate": "20250901",
                        "endDate": "20250903",
                        "buyerName": "테스트",
                        "totalAmount": 100000,
                        "state": 1,
                        "registeredTs": 1723456789
                    }
                    """;

                given(mapper.fromYeogieottae(any(), any()))
                        .willReturn(ReservationEvent.builder()
                                .eventId(UUID.randomUUID())
                                .platformType(PlatformType.YEOGIEOTTAE)
                                .status(ReservationStatus.CONFIRMED)
                                .receivedAt(Instant.now())
                                .build());

                // when
                translator.translate(json, EventType.BOOKING);

                // then
                then(mapper).should().fromYeogieottae(
                        org.mockito.ArgumentMatchers.argThat(payload -> payload.getState() == 1),
                        any(TranslationContext.class)
                );
            }

            @Test
            @DisplayName("state=2이면 DTO에 2가 파싱된다")
            void it_parses_state_2() {
                // given
                String json = """
                    {
                        "orderId": "YEO-STATE002",
                        "roomTypeId": "RT-102",
                        "accommodationId": "ACC-002",
                        "startDate": "20250901",
                        "endDate": "20250903",
                        "buyerName": "테스트",
                        "totalAmount": 100000,
                        "state": 2,
                        "registeredTs": 1723456789
                    }
                    """;

                given(mapper.fromYeogieottae(any(), any()))
                        .willReturn(ReservationEvent.builder()
                                .eventId(UUID.randomUUID())
                                .platformType(PlatformType.YEOGIEOTTAE)
                                .status(ReservationStatus.CANCELLED)
                                .receivedAt(Instant.now())
                                .build());

                // when
                translator.translate(json, EventType.CANCELLATION);

                // then
                then(mapper).should().fromYeogieottae(
                        org.mockito.ArgumentMatchers.argThat(payload -> payload.getState() == 2),
                        any(TranslationContext.class)
                );
            }
        }
    }

    @Nested
    @DisplayName("getPlatformType 메서드는")
    class Describe_getPlatformType {

        @Test
        @DisplayName("YEOGIEOTTAE를 반환한다")
        void it_returns_yeogieottae() {
            assertThat(translator.getPlatformType()).isEqualTo(PlatformType.YEOGIEOTTAE);
        }
    }

    @Nested
    @DisplayName("getDtoClass 메서드는")
    class Describe_getDtoClass {

        @Test
        @DisplayName("YeogieottaePayload.class를 반환한다")
        void it_returns_yeogieottae_payload_class() {
            assertThat(translator.getDtoClass()).isEqualTo(YeogieottaePayload.class);
        }
    }
}
