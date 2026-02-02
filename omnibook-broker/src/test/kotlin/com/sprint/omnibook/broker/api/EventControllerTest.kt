package com.sprint.omnibook.broker.api

import com.sprint.omnibook.broker.api.dto.ErrorResponse
import com.sprint.omnibook.broker.api.dto.EventResponse
import com.sprint.omnibook.broker.api.exception.ErrorCode
import com.sprint.omnibook.broker.ingestion.EventHeaders
import com.sprint.omnibook.broker.ingestion.EventIngestionService
import com.sprint.omnibook.broker.ingestion.IngestionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
@DisplayName("EventController")
class EventControllerTest {

    @Mock
    private lateinit var ingestionService: EventIngestionService

    private lateinit var controller: EventController

    @BeforeEach
    fun setUp() {
        controller = EventController(ingestionService)
    }

    @Nested
    @DisplayName("receiveEvent 메서드는")
    inner class Describe_receiveEvent {

        @Nested
        @DisplayName("유효한 이벤트가 주어지면")
        inner class Context_with_valid_event {

            @Test
            @DisplayName("ACCEPTED 응답을 반환한다")
            fun it_returns_accepted() {
                // given
                val rawBody = """
                    {
                        "eventId": "evt-123",
                        "reservationId": "YNJ-12345678",
                        "payload": {"roomId": "R-101"}
                    }
                """.trimIndent()

                given(ingestionService.process(eq(rawBody), any()))
                    .willReturn(IngestionResult.success("evt-123"))

                // when
                val response = controller.receiveEvent(
                    "evt-123", "A", "BOOKING", "corr-456", rawBody)

                // then
                assertThat(response.statusCode.value()).isEqualTo(200)
                assertThat(response.body).isInstanceOf(EventResponse::class.java)

                val eventResponse = response.body as EventResponse
                assertThat(eventResponse.eventId).isEqualTo("evt-123")
                assertThat(eventResponse.status).isEqualTo("ACCEPTED")

                val headersCaptor = argumentCaptor<EventHeaders>()
                verify(ingestionService).process(eq(rawBody), headersCaptor.capture())

                val captured = headersCaptor.firstValue
                assertThat(captured.eventId).isEqualTo("evt-123")
                assertThat(captured.platform).isEqualTo("A")
                assertThat(captured.eventType).isEqualTo("BOOKING")
                assertThat(captured.correlationId).isEqualTo("corr-456")
            }
        }

        @Nested
        @DisplayName("변환 실패 시")
        inner class Context_when_translation_fails {

            @Test
            @DisplayName("ErrorResponse를 반환한다")
            fun it_returns_error_response() {
                // given
                val rawBody = """
                    {
                        "eventId": "evt-fail",
                        "reservationId": "INVALID",
                        "payload": {}
                    }
                """.trimIndent()

                given(ingestionService.process(eq(rawBody), any()))
                    .willReturn(IngestionResult.failure("evt-fail", "지원하지 않는 플랫폼입니다.", ErrorCode.INVALID_PLATFORM))

                // when
                val response = controller.receiveEvent(
                    null, "UNKNOWN", "BOOKING", null, rawBody)

                // then
                assertThat(response.statusCode.value()).isEqualTo(400)
                assertThat(response.body).isInstanceOf(ErrorResponse::class.java)

                val errorResponse = response.body as ErrorResponse
                assertThat(errorResponse.code).isEqualTo("INVALID_PLATFORM")
                assertThat(errorResponse.reason).isEqualTo("지원하지 않는 플랫폼입니다.")
                assertThat(errorResponse.details).containsEntry("eventId", "evt-fail")
            }
        }
    }
}
