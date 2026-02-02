package com.sprint.omnibook.broker.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.api.exception.ErrorCode
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import com.sprint.omnibook.broker.event.ReservationStatus
import com.sprint.omnibook.broker.persistence.RawEventService
import com.sprint.omnibook.broker.processing.FailureReason
import com.sprint.omnibook.broker.processing.ProcessingResult
import com.sprint.omnibook.broker.processing.ReservationProcessingService
import com.sprint.omnibook.broker.translator.PayloadTranslator
import com.sprint.omnibook.broker.translator.TranslationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("EventIngestionService")
class EventIngestionServiceTest {

    @Mock
    private lateinit var rawEventService: RawEventService

    @Mock
    private lateinit var yanoljaTranslator: PayloadTranslator

    @Mock
    private lateinit var airbnbTranslator: PayloadTranslator

    @Mock
    private lateinit var yeogieottaeTranslator: PayloadTranslator

    @Mock
    private lateinit var reservationProcessingService: ReservationProcessingService

    private lateinit var failedEventStore: FailedEventStore
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: EventIngestionService

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        failedEventStore = FailedEventStore()

        val translators = EnumMap<PlatformType, PayloadTranslator>(PlatformType::class.java).apply {
            put(PlatformType.YANOLJA, yanoljaTranslator)
            put(PlatformType.AIRBNB, airbnbTranslator)
            put(PlatformType.YEOGIEOTTAE, yeogieottaeTranslator)
        }

        service = EventIngestionService(
            rawEventService,
            translators,
            failedEventStore,
            objectMapper,
            reservationProcessingService
        )
    }

    @Nested
    @DisplayName("ingest 메서드는")
    inner class Describe_ingest {

        @Nested
        @DisplayName("유효한 요청이 주어지면")
        inner class Context_with_valid_request {

            @ParameterizedTest
            @CsvSource(
                "A, YANOLJA",
                "B, AIRBNB",
                "C, YEOGIEOTTAE",
                "YANOLJA, YANOLJA",
                "AIRBNB, AIRBNB",
                "YEOGIEOTTAE, YEOGIEOTTAE"
            )
            @DisplayName("플랫폼 헤더를 올바르게 매핑한다")
            fun it_maps_platform_correctly(header: String, expected: PlatformType) {
                // given
                val payload = objectMapper.readTree("""{"reservationId": "TEST-001"}""")
                val request = IngestRequest(
                    "evt-1", header, "BOOKING", "corr-1", "res-1", payload
                )

                val expectedTranslator = when (expected) {
                    PlatformType.YANOLJA -> yanoljaTranslator
                    PlatformType.AIRBNB -> airbnbTranslator
                    PlatformType.YEOGIEOTTAE -> yeogieottaeTranslator
                }

                given(expectedTranslator.translate(any(), any())).willReturn(createMockEvent())
                given(reservationProcessingService.process(any()))
                    .willReturn(ProcessingResult.success(null, null))

                // when
                val result = service.ingest(request)

                // then
                assertThat(result.success).isTrue()
                verify(expectedTranslator).translate(any(), eq(EventType.BOOKING))
            }

            @ParameterizedTest
            @CsvSource(
                "BOOKING, BOOKING",
                "CANCEL, CANCELLATION",
                "CANCELLATION, CANCELLATION"
            )
            @DisplayName("이벤트 타입을 올바르게 매핑한다")
            fun it_maps_event_type_correctly(header: String, expected: EventType) {
                // given
                val payload = objectMapper.readTree("""{"test": true}""")
                val request = IngestRequest(
                    "evt-1", "A", header, "corr-1", "res-1", payload
                )

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent())
                given(reservationProcessingService.process(any()))
                    .willReturn(ProcessingResult.success(null, null))

                // when
                service.ingest(request)

                // then
                verify(yanoljaTranslator).translate(any(), eq(expected))
            }
        }

        @Nested
        @DisplayName("알 수 없는 플랫폼이 주어지면")
        inner class Context_with_unknown_platform {

            @Test
            @DisplayName("실패를 반환하고 원본을 저장한다")
            fun it_saves_failed_event() {
                // given
                val payload = objectMapper.readTree("""{"test": true}""")
                val request = IngestRequest(
                    "evt-1", "UNKNOWN", "BOOKING", "corr-1", "res-1", payload
                )

                // when
                val result = service.ingest(request)

                // then
                assertThat(result.success).isFalse()
                assertThat(result.failureReason).contains("알 수 없는 플랫폼")
                assertThat(result.errorCode).isEqualTo(ErrorCode.INVALID_PLATFORM)
                assertThat(failedEventStore.count()).isEqualTo(1)
                assertThat(failedEventStore.findAll()[0].errorMessage)
                    .contains("알 수 없는 플랫폼")
            }
        }

        @Nested
        @DisplayName("Translator가 예외를 던지면")
        inner class Context_when_translator_throws {

            @Test
            @DisplayName("실패를 반환하고 원본을 저장한다")
            fun it_saves_failed_event() {
                // given
                val payload = objectMapper.readTree("""{"invalid": "data"}""")
                val request = IngestRequest(
                    "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                )

                given(yanoljaTranslator.translate(any(), any()))
                    .willThrow(TranslationException("파싱 실패"))

                // when
                val result = service.ingest(request)

                // then
                assertThat(result.success).isFalse()
                assertThat(result.failureReason).isEqualTo("파싱 실패")
                assertThat(result.errorCode).isEqualTo(ErrorCode.EVENT_PARSE_ERROR)
                assertThat(failedEventStore.count()).isEqualTo(1)
                val failed = failedEventStore.findAll()[0]
                assertThat(failed.eventId).isEqualTo("evt-1")
                assertThat(failed.errorMessage).isEqualTo("파싱 실패")
            }
        }

        @Nested
        @DisplayName("ReservationProcessingService 호출 시")
        inner class Context_when_calling_processing_service {

            @Test
            @DisplayName("처리 성공 시 true를 반환한다")
            fun it_returns_true_on_success() {
                // given
                val payload = objectMapper.readTree("""{"test": true}""")
                val request = IngestRequest(
                    "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                )

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent())
                given(reservationProcessingService.process(any()))
                    .willReturn(ProcessingResult.success(null, null))

                // when
                val result = service.ingest(request)

                // then
                assertThat(result.success).isTrue()
                verify(reservationProcessingService).process(any())
            }

            @Test
            @DisplayName("처리 실패 시 false를 반환한다")
            fun it_returns_false_on_failure() {
                // given
                val payload = objectMapper.readTree("""{"test": true}""")
                val request = IngestRequest(
                    "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                )

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent())
                given(reservationProcessingService.process(any()))
                    .willReturn(ProcessingResult.failure(FailureReason.UNKNOWN_ROOM))

                // when
                val result = service.ingest(request)

                // then
                assertThat(result.success).isFalse()
                assertThat(result.failureReason).isEqualTo("UNKNOWN_ROOM")
                assertThat(result.errorCode).isEqualTo(ErrorCode.UNKNOWN_ROOM)
                // FailedEventStore에는 저장하지 않음 (ReservationEventEntity에 기록됨)
                assertThat(failedEventStore.count()).isEqualTo(0)
            }
        }
    }

    @Nested
    @DisplayName("process 메서드는")
    inner class Describe_process {

        @Test
        @DisplayName("RawEventService.store를 호출하고 결과를 반환한다")
        fun it_calls_raw_event_service_and_returns_result() {
            // given
            val rawBody = """
                {
                    "eventId": "evt-123",
                    "reservationId": "YNJ-12345678",
                    "payload": {"roomId": "R-101"}
                }
                """.trimIndent()
            val headers = EventHeaders("evt-123", "A", "BOOKING", "corr-456")

            given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent())
            given(reservationProcessingService.process(any()))
                .willReturn(ProcessingResult.success(null, null))

            // when
            val result = service.process(rawBody, headers)

            // then
            assertThat(result.eventId).isEqualTo("evt-123")
            assertThat(result.success).isTrue()
            verify(rawEventService).store(rawBody, headers)
        }

        @Test
        @DisplayName("파싱 실패 시에도 원본을 저장하고 실패를 반환한다")
        fun it_stores_raw_event_even_when_parsing_fails() {
            // given
            val invalidRawBody = "{ invalid json }}}"
            val headers = EventHeaders("evt-123", "A", "BOOKING", "corr-456")

            // when
            val result = service.process(invalidRawBody, headers)

            // then
            assertThat(result.success).isFalse()
            assertThat(result.errorCode).isEqualTo(ErrorCode.EVENT_PARSE_ERROR)
            verify(rawEventService).store(invalidRawBody, headers)
            assertThat(failedEventStore.count()).isEqualTo(1)
            assertThat(failedEventStore.findAll()[0].errorMessage)
                .contains("JSON 파싱 실패")
        }
    }

    private fun createMockEvent(): ReservationEvent {
        return ReservationEvent(
            eventId = UUID.randomUUID(),
            platformType = PlatformType.YANOLJA,
            status = ReservationStatus.CONFIRMED,
            receivedAt = Instant.now()
        )
    }
}
