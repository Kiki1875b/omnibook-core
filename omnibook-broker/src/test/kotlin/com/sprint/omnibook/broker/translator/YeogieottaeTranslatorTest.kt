package com.sprint.omnibook.broker.translator

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.event.ReservationStatus
import com.sprint.omnibook.broker.translator.dto.YeogieottaePayload
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
@DisplayName("YeogieottaeTranslator")
class YeogieottaeTranslatorTest {

    @Mock
    private lateinit var mapper: ReservationEventMapper

    private lateinit var objectMapper: ObjectMapper
    private lateinit var translator: YeogieottaeTranslator

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        translator = YeogieottaeTranslator(objectMapper, mapper)
    }

    @Nested
    @DisplayName("translate 메서드는")
    inner class Describe_translate {

        @Nested
        @DisplayName("유효한 YeogiEottae payload가 주어지면")
        inner class Context_with_valid_payload {

            private val registeredTs = 1723456789L
            private val validJson = """
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
                    "registeredTs": $registeredTs
                }
                """.trimIndent()

            private lateinit var mockEvent: ReservationEvent

            @BeforeEach
            fun setUp() {
                mockEvent = ReservationEvent(
                    eventId = UUID.randomUUID(),
                    platformType = PlatformType.YEOGIEOTTAE,
                    platformReservationId = "YEO-12345678",
                    eventType = EventType.BOOKING,
                    roomId = "RT-101",
                    propertyName = "ACC-001",
                    checkIn = LocalDate.of(2025, 8, 15),
                    checkOut = LocalDate.of(2025, 8, 18),
                    guestName = "김민수",
                    guestPhone = "01012345678",
                    totalAmount = BigDecimal.valueOf(380000),
                    status = ReservationStatus.CONFIRMED,
                    occurredAt = Instant.ofEpochSecond(registeredTs),
                    receivedAt = Instant.now(),
                    rawPayload = validJson
                )

                given(mapper.fromYeogieottae(any<YeogieottaePayload>(), any<TranslationContext>()))
                    .willReturn(mockEvent)
            }

            @Test
            @DisplayName("ReservationEvent를 반환한다")
            fun it_returns_reservation_event() {
                // when
                val result = translator.translate(validJson, EventType.BOOKING)

                // then
                assertThat(result).isNotNull
                assertThat(result.platformType).isEqualTo(PlatformType.YEOGIEOTTAE)
                assertThat(result.platformReservationId).isEqualTo("YEO-12345678")
            }

            @Test
            @DisplayName("Mapper의 fromYeogieottae를 호출한다")
            fun it_calls_mapper_fromYeogieottae() {
                // when
                translator.translate(validJson, EventType.BOOKING)

                // then
                verify(mapper).fromYeogieottae(any<YeogieottaePayload>(), any<TranslationContext>())
            }

            @Test
            @DisplayName("DTO에 compact 날짜 형식(yyyyMMdd)이 파싱된다")
            fun it_parses_compact_date_format() {
                // when
                translator.translate(validJson, EventType.BOOKING)

                // then
                verify(mapper).fromYeogieottae(
                    argThat<YeogieottaePayload> { payload ->
                        payload.startDate == "20250815" && payload.endDate == "20250818"
                    },
                    any<TranslationContext>()
                )
            }

            @Test
            @DisplayName("DTO에 epoch seconds 타임스탬프가 파싱된다")
            fun it_parses_epoch_seconds() {
                // when
                translator.translate(validJson, EventType.BOOKING)

                // then
                verify(mapper).fromYeogieottae(
                    argThat<YeogieottaePayload> { payload ->
                        payload.registeredTs == registeredTs
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
                val invalidJson = "{ broken json"

                // when & then
                assertThatThrownBy { translator.translate(invalidJson, EventType.BOOKING) }
                    .isInstanceOf(TranslationException::class.java)
                    .hasMessageContaining("YEOGIEOTTAE")
                    .hasMessageContaining("변환 실패")
            }
        }

        @Nested
        @DisplayName("state 필드 값에 따라")
        inner class Context_with_different_states {

            @Test
            @DisplayName("state=1이면 DTO에 1이 파싱된다")
            fun it_parses_state_1() {
                // given
                val json = """
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
                    """.trimIndent()

                given(mapper.fromYeogieottae(any(), any()))
                    .willReturn(
                        ReservationEvent(
                            eventId = UUID.randomUUID(),
                            platformType = PlatformType.YEOGIEOTTAE,
                            status = ReservationStatus.CONFIRMED,
                            receivedAt = Instant.now()
                        )
                    )

                // when
                translator.translate(json, EventType.BOOKING)

                // then
                verify(mapper).fromYeogieottae(
                    argThat<YeogieottaePayload> { payload -> payload.state == 1 },
                    any<TranslationContext>()
                )
            }

            @Test
            @DisplayName("state=2이면 DTO에 2가 파싱된다")
            fun it_parses_state_2() {
                // given
                val json = """
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
                    """.trimIndent()

                given(mapper.fromYeogieottae(any(), any()))
                    .willReturn(
                        ReservationEvent(
                            eventId = UUID.randomUUID(),
                            platformType = PlatformType.YEOGIEOTTAE,
                            status = ReservationStatus.CANCELLED,
                            receivedAt = Instant.now()
                        )
                    )

                // when
                translator.translate(json, EventType.CANCELLATION)

                // then
                verify(mapper).fromYeogieottae(
                    argThat<YeogieottaePayload> { payload -> payload.state == 2 },
                    any<TranslationContext>()
                )
            }
        }
    }

}
