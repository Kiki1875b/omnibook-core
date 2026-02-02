package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.event.ReservationStatus
import com.sprint.omnibook.broker.translator.dto.AirbnbPayload
import com.sprint.omnibook.broker.translator.mapper.ReservationEventMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AirbnbTranslator")
class AirbnbTranslatorTest {

    @Mock
    private lateinit var mapper: ReservationEventMapper

    private lateinit var objectMapper: ObjectMapper
    private lateinit var translator: AirbnbTranslator

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        translator = AirbnbTranslator(objectMapper, mapper)
    }

    @Nested
    @DisplayName("translate 메서드는")
    inner class Describe_translate {

        @Nested
        @DisplayName("유효한 Airbnb payload가 주어지면")
        inner class Context_with_valid_payload {

            private val createdAt = 1723456789000L
            private val validJson = """
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
                    "createdAt": $createdAt
                }
                """.trimIndent()

            private lateinit var mockEvent: ReservationEvent

            @BeforeEach
            fun setUp() {
                mockEvent = ReservationEvent(
                    eventId = UUID.randomUUID(),
                    platformType = PlatformType.AIRBNB,
                    platformReservationId = "ABC123XYZW",
                    eventType = EventType.BOOKING,
                    roomId = "listing-001",
                    checkIn = LocalDate.of(2025, 8, 15),
                    checkOut = LocalDate.of(2025, 8, 18),
                    guestName = "Minsu Kim",
                    guestEmail = "minsu@example.com",
                    totalAmount = BigDecimal.valueOf(350.5),
                    status = ReservationStatus.CONFIRMED,
                    occurredAt = Instant.ofEpochMilli(createdAt),
                    receivedAt = Instant.now(),
                    rawPayload = validJson
                )

                given(mapper.fromAirbnb(any<AirbnbPayload>(), any<TranslationContext>()))
                    .willReturn(mockEvent)
            }

            @Test
            @DisplayName("ReservationEvent를 반환한다")
            fun it_returns_reservation_event() {
                // when
                val result = translator.translate(validJson, EventType.BOOKING)

                // then
                assertThat(result).isNotNull
                assertThat(result.platformType).isEqualTo(PlatformType.AIRBNB)
                assertThat(result.platformReservationId).isEqualTo("ABC123XYZW")
            }

            @Test
            @DisplayName("Mapper의 fromAirbnb를 호출한다")
            fun it_calls_mapper_fromAirbnb() {
                // when
                translator.translate(validJson, EventType.BOOKING)

                // then
                verify(mapper).fromAirbnb(any<AirbnbPayload>(), any<TranslationContext>())
            }

            @Test
            @DisplayName("DTO에 firstName과 lastName이 파싱된다")
            fun it_parses_guest_names() {
                // when
                translator.translate(validJson, EventType.BOOKING)

                // then
                verify(mapper).fromAirbnb(
                    argThat<AirbnbPayload> { payload ->
                        payload.guestFirstName == "Minsu" && payload.guestLastName == "Kim"
                    },
                    any<TranslationContext>()
                )
            }
        }

        @Nested
        @DisplayName("잘못된 JSON이 주어지면")
        inner class Context_with_invalid_json {

            @Test
            @DisplayName("TranslationException을 던진다")
            fun it_throws_translation_exception() {
                // given
                val invalidJson = "not a json"

                // when & then
                assertThatThrownBy { translator.translate(invalidJson, EventType.BOOKING) }
                    .isInstanceOf(TranslationException::class.java)
                    .hasMessageContaining("AIRBNB")
                    .hasMessageContaining("변환 실패")
            }
        }

        @Nested
        @DisplayName("CANCELLATION eventType이 주어지면")
        inner class Context_with_cancellation_event_type {

            @Test
            @DisplayName("TranslationContext에 CANCELLATION을 전달한다")
            fun it_passes_cancellation_event_type() {
                // given
                val json = """
                    {
                        "confirmationCode": "CANCEL001",
                        "listingId": "listing-001",
                        "checkIn": "2025-09-01",
                        "checkOut": "2025-09-03",
                        "totalPayout": 200.0,
                        "status": "CANCELLED",
                        "createdAt": 1723456789000
                    }
                    """.trimIndent()

                given(mapper.fromAirbnb(any(), any()))
                    .willReturn(
                        ReservationEvent(
                            eventId = UUID.randomUUID(),
                            platformType = PlatformType.AIRBNB,
                            platformReservationId = "CANCEL001",
                            eventType = EventType.CANCELLATION,
                            status = ReservationStatus.CANCELLED,
                            receivedAt = Instant.now()
                        )
                    )

                // when
                translator.translate(json, EventType.CANCELLATION)

                // then
                verify(mapper).fromAirbnb(
                    any<AirbnbPayload>(),
                    argThat<TranslationContext> { ctx ->
                        ctx.eventType == EventType.CANCELLATION
                    }
                )
            }
        }
    }

}
