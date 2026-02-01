package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.translator.dto.YanoljaPayload;
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
@DisplayName("YanoljaTranslator")
class YanoljaTranslatorTest {

    @Mock
    private ReservationEventMapper mapper;

    private ObjectMapper objectMapper;
    private YanoljaTranslator translator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        translator = new YanoljaTranslator(objectMapper, mapper);
    }

    @Nested
    @DisplayName("translate 메서드는")
    class Describe_translate {

        @Nested
        @DisplayName("유효한 Yanolja payload가 주어지면")
        class Context_with_valid_payload {

            private final String validJson = """
                {
                    "reservationId": "YNJ-12345678",
                    "roomId": "R-101",
                    "accommodationName": "서울 호텔",
                    "checkInDate": "2025-08-15",
                    "checkOutDate": "2025-08-18",
                    "guestName": "김민수",
                    "guestPhone": "010-1234-5678",
                    "totalPrice": 450000,
                    "status": "예약완료",
                    "bookedAt": "2025-08-01T10:30:00"
                }
                """;

            private ReservationEvent mockEvent;

            @BeforeEach
            void setUp() {
                mockEvent = ReservationEvent.builder()
                        .eventId(UUID.randomUUID())
                        .platformType(PlatformType.YANOLJA)
                        .platformReservationId("YNJ-12345678")
                        .eventType(EventType.BOOKING)
                        .roomId("R-101")
                        .propertyName("서울 호텔")
                        .checkIn(LocalDate.of(2025, 8, 15))
                        .checkOut(LocalDate.of(2025, 8, 18))
                        .guestName("김민수")
                        .guestPhone("010-1234-5678")
                        .totalAmount(BigDecimal.valueOf(450000))
                        .status(ReservationStatus.CONFIRMED)
                        .occurredAt(Instant.now())
                        .receivedAt(Instant.now())
                        .rawPayload(validJson)
                        .build();

                given(mapper.fromYanolja(any(YanoljaPayload.class), any(TranslationContext.class)))
                        .willReturn(mockEvent);
            }

            @Test
            @DisplayName("ReservationEvent를 반환한다")
            void it_returns_reservation_event() {
                // when
                ReservationEvent result = translator.translate(validJson, EventType.BOOKING);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPlatformType()).isEqualTo(PlatformType.YANOLJA);
                assertThat(result.getPlatformReservationId()).isEqualTo("YNJ-12345678");
            }

            @Test
            @DisplayName("Mapper의 fromYanolja를 호출한다")
            void it_calls_mapper_fromYanolja() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromYanolja(any(YanoljaPayload.class), any(TranslationContext.class));
            }

            @Test
            @DisplayName("TranslationContext에 eventType과 rawPayload를 전달한다")
            void it_passes_translation_context() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromYanolja(
                        any(YanoljaPayload.class),
                        org.mockito.ArgumentMatchers.argThat(ctx ->
                                ctx.eventType() == EventType.BOOKING &&
                                ctx.rawPayload().equals(validJson)
                        )
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
                String invalidJson = "{ invalid json }";

                // when & then
                assertThatThrownBy(() -> translator.translate(invalidJson, EventType.BOOKING))
                        .isInstanceOf(TranslationException.class)
                        .hasMessageContaining("YANOLJA")
                        .hasMessageContaining("변환 실패");
            }
        }

        @Nested
        @DisplayName("Mapper에서 예외가 발생하면")
        class Context_when_mapper_throws_exception {

            @Test
            @DisplayName("TranslationException으로 래핑하여 던진다")
            void it_wraps_exception() {
                // given
                String validJson = """
                    {
                        "reservationId": "YNJ-ERROR001",
                        "roomId": "R-101",
                        "checkInDate": "2025-08-15",
                        "checkOutDate": "2025-08-18",
                        "guestName": "테스트",
                        "totalPrice": 100000,
                        "status": "예약완료"
                    }
                    """;

                given(mapper.fromYanolja(any(), any()))
                        .willThrow(new RuntimeException("Mapper error"));

                // when & then
                assertThatThrownBy(() -> translator.translate(validJson, EventType.BOOKING))
                        .isInstanceOf(TranslationException.class)
                        .hasMessageContaining("YANOLJA")
                        .hasCauseInstanceOf(RuntimeException.class);
            }
        }
    }

    @Nested
    @DisplayName("getPlatformType 메서드는")
    class Describe_getPlatformType {

        @Test
        @DisplayName("YANOLJA를 반환한다")
        void it_returns_yanolja() {
            assertThat(translator.getPlatformType()).isEqualTo(PlatformType.YANOLJA);
        }
    }

    @Nested
    @DisplayName("getDtoClass 메서드는")
    class Describe_getDtoClass {

        @Test
        @DisplayName("YanoljaPayload.class를 반환한다")
        void it_returns_yanolja_payload_class() {
            assertThat(translator.getDtoClass()).isEqualTo(YanoljaPayload.class);
        }
    }
}
