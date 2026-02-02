package com.sprint.omnibook.broker.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload;
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
@DisplayName("AirbnbTranslator")
class AirbnbTranslatorTest {

    @Mock
    private ReservationEventMapper mapper;

    private ObjectMapper objectMapper;
    private AirbnbTranslator translator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        translator = new AirbnbTranslator(objectMapper, mapper);
    }

    @Nested
    @DisplayName("translate 메서드는")
    class Describe_translate {

        @Nested
        @DisplayName("유효한 Airbnb payload가 주어지면")
        class Context_with_valid_payload {

            private final long createdAt = 1723456789000L;
            private final String validJson = """
                {
                    "confirmationCode": "ABC123XYZW",
                    "listingId": "listing-001",
                    "hostId": "host-999",
                    "checkIn": "2025-08-15",
                    "checkOut": "2025-08-18",
                    "guestFirstName": "Minsu",
                    "guestLastName": "Kim",
                    "guestEmail": "minsu@example.com",
                    "totalPayout": 350.50,
                    "status": "ACCEPTED",
                    "createdAt": %d
                }
                """.formatted(createdAt);

            private ReservationEvent mockEvent;

            @BeforeEach
            void setUp() {
                mockEvent = ReservationEvent.builder()
                        .eventId(UUID.randomUUID())
                        .platformType(PlatformType.AIRBNB)
                        .platformReservationId("ABC123XYZW")
                        .eventType(EventType.BOOKING)
                        .roomId("listing-001")
                        .checkIn(LocalDate.of(2025, 8, 15))
                        .checkOut(LocalDate.of(2025, 8, 18))
                        .guestName("Minsu Kim")
                        .guestEmail("minsu@example.com")
                        .totalAmount(BigDecimal.valueOf(350.5))
                        .status(ReservationStatus.CONFIRMED)
                        .occurredAt(Instant.ofEpochMilli(createdAt))
                        .receivedAt(Instant.now())
                        .rawPayload(validJson)
                        .build();

                given(mapper.fromAirbnb(any(AirbnbPayload.class), any(TranslationContext.class)))
                        .willReturn(mockEvent);
            }

            @Test
            @DisplayName("ReservationEvent를 반환한다")
            void it_returns_reservation_event() {
                // when
                ReservationEvent result = translator.translate(validJson, EventType.BOOKING);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPlatformType()).isEqualTo(PlatformType.AIRBNB);
                assertThat(result.getPlatformReservationId()).isEqualTo("ABC123XYZW");
            }

            @Test
            @DisplayName("Mapper의 fromAirbnb를 호출한다")
            void it_calls_mapper_fromAirbnb() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromAirbnb(any(AirbnbPayload.class), any(TranslationContext.class));
            }

            @Test
            @DisplayName("DTO에 firstName과 lastName이 파싱된다")
            void it_parses_guest_names() {
                // when
                translator.translate(validJson, EventType.BOOKING);

                // then
                then(mapper).should().fromAirbnb(
                        org.mockito.ArgumentMatchers.argThat(payload ->
                                "Minsu".equals(payload.getGuestFirstName()) &&
                                "Kim".equals(payload.getGuestLastName())
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
                String invalidJson = "not a json";

                // when & then
                assertThatThrownBy(() -> translator.translate(invalidJson, EventType.BOOKING))
                        .isInstanceOf(TranslationException.class)
                        .hasMessageContaining("AIRBNB")
                        .hasMessageContaining("변환 실패");
            }
        }

        @Nested
        @DisplayName("CANCELLATION eventType이 주어지면")
        class Context_with_cancellation_event_type {

            @Test
            @DisplayName("TranslationContext에 CANCELLATION을 전달한다")
            void it_passes_cancellation_event_type() {
                // given
                String json = """
                    {
                        "confirmationCode": "CANCEL001",
                        "listingId": "listing-001",
                        "checkIn": "2025-09-01",
                        "checkOut": "2025-09-03",
                        "totalPayout": 200.0,
                        "status": "CANCELLED",
                        "createdAt": 1723456789000
                    }
                    """;

                given(mapper.fromAirbnb(any(), any()))
                        .willReturn(ReservationEvent.builder()
                                .eventId(UUID.randomUUID())
                                .platformType(PlatformType.AIRBNB)
                                .platformReservationId("CANCEL001")
                                .eventType(EventType.CANCELLATION)
                                .status(ReservationStatus.CANCELLED)
                                .receivedAt(Instant.now())
                                .build());

                // when
                translator.translate(json, EventType.CANCELLATION);

                // then
                then(mapper).should().fromAirbnb(
                        any(AirbnbPayload.class),
                        org.mockito.ArgumentMatchers.argThat(ctx ->
                                ctx.eventType() == EventType.CANCELLATION
                        )
                );
            }
        }
    }

    @Nested
    @DisplayName("getPlatformType 메서드는")
    class Describe_getPlatformType {

        @Test
        @DisplayName("AIRBNB를 반환한다")
        void it_returns_airbnb() {
            assertThat(translator.getPlatformType()).isEqualTo(PlatformType.AIRBNB);
        }
    }

    @Nested
    @DisplayName("getDtoClass 메서드는")
    class Describe_getDtoClass {

        @Test
        @DisplayName("AirbnbPayload.class를 반환한다")
        void it_returns_airbnb_payload_class() {
            assertThat(translator.getDtoClass()).isEqualTo(AirbnbPayload.class);
        }
    }
}
