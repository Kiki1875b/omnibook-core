package com.sprint.omnibook.broker.api;

import com.sprint.omnibook.broker.api.dto.EventResponse;
import com.sprint.omnibook.broker.ingestion.EventHeaders;
import com.sprint.omnibook.broker.ingestion.EventIngestionService;
import com.sprint.omnibook.broker.ingestion.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventController")
class EventControllerTest {

    @Mock
    private EventIngestionService ingestionService;

    private EventController controller;

    @BeforeEach
    void setUp() {
        controller = new EventController(ingestionService);
    }

    @Nested
    @DisplayName("receiveEvent 메서드는")
    class Describe_receiveEvent {

        @Nested
        @DisplayName("유효한 이벤트가 주어지면")
        class Context_with_valid_event {

            @Test
            @DisplayName("ACCEPTED 응답을 반환한다")
            void it_returns_accepted() throws Exception {
                // given
                String rawBody = """
                        {
                            "eventId": "evt-123",
                            "reservationId": "YNJ-12345678",
                            "payload": {"roomId": "R-101"}
                        }
                        """;

                given(ingestionService.process(eq(rawBody), any(EventHeaders.class)))
                        .willReturn(new IngestionResult("evt-123", true));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        "evt-123", "A", "BOOKING", "corr-456", rawBody);

                // then
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getEventId()).isEqualTo("evt-123");
                assertThat(response.getBody().getStatus()).isEqualTo("ACCEPTED");

                ArgumentCaptor<EventHeaders> headersCaptor = ArgumentCaptor.forClass(EventHeaders.class);
                then(ingestionService).should().process(eq(rawBody), headersCaptor.capture());

                EventHeaders captured = headersCaptor.getValue();
                assertThat(captured.eventId()).isEqualTo("evt-123");
                assertThat(captured.platform()).isEqualTo("A");
                assertThat(captured.eventType()).isEqualTo("BOOKING");
                assertThat(captured.correlationId()).isEqualTo("corr-456");
            }
        }

        @Nested
        @DisplayName("변환 실패 시")
        class Context_when_translation_fails {

            @Test
            @DisplayName("SAVED_FOR_RETRY 응답을 반환한다")
            void it_returns_saved_for_retry() throws Exception {
                // given
                String rawBody = """
                        {
                            "eventId": "evt-fail",
                            "reservationId": "INVALID",
                            "payload": {}
                        }
                        """;

                given(ingestionService.process(eq(rawBody), any(EventHeaders.class)))
                        .willReturn(new IngestionResult("evt-fail", false));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        null, "A", "BOOKING", null, rawBody);

                // then
                assertThat(response.getStatusCode().value()).isEqualTo(202);
                assertThat(response.getBody().getEventId()).isEqualTo("evt-fail");
                assertThat(response.getBody().getStatus()).isEqualTo("SAVED_FOR_RETRY");
            }
        }
    }
}
