package com.sprint.omnibook.broker.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.api.dto.EventResponse;
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest;
import com.sprint.omnibook.broker.ingestion.EventIngestionService;
import com.sprint.omnibook.broker.ingestion.IngestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventController")
class EventControllerTest {

    @Mock
    private EventIngestionService ingestionService;

    private ObjectMapper objectMapper;
    private EventController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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
                given(ingestionService.ingest(any(IngestRequest.class))).willReturn(true);

                IncomingEventRequest request = new IncomingEventRequest();
                request.setEventId("evt-123");
                request.setReservationId("YNJ-12345678");
                request.setPayload(objectMapper.readTree("{\"roomId\": \"R-101\"}"));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        "evt-123", "A", "BOOKING", "corr-456", request);

                // then
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getEventId()).isEqualTo("evt-123");
                assertThat(response.getBody().getStatus()).isEqualTo("ACCEPTED");
                then(ingestionService).should().ingest(any(IngestRequest.class));
            }
        }

        @Nested
        @DisplayName("변환 실패 시")
        class Context_when_translation_fails {

            @Test
            @DisplayName("SAVED_FOR_RETRY 응답을 반환한다")
            void it_returns_saved_for_retry() throws Exception {
                // given
                given(ingestionService.ingest(any(IngestRequest.class))).willReturn(false);

                IncomingEventRequest request = new IncomingEventRequest();
                request.setEventId("evt-fail");
                request.setReservationId("INVALID");
                request.setPayload(objectMapper.readTree("{}"));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        null, "A", "BOOKING", null, request);

                // then
                assertThat(response.getStatusCode().value()).isEqualTo(202);
                assertThat(response.getBody().getEventId()).isEqualTo("evt-fail");
                assertThat(response.getBody().getStatus()).isEqualTo("SAVED_FOR_RETRY");
            }
        }

        @Nested
        @DisplayName("eventId가 헤더에 없고 바디에만 있으면")
        class Context_with_body_event_id_only {

            @Test
            @DisplayName("바디의 eventId를 사용한다")
            void it_uses_body_event_id() throws Exception {
                // given
                given(ingestionService.ingest(any(IngestRequest.class))).willReturn(true);

                IncomingEventRequest request = new IncomingEventRequest();
                request.setEventId("body-evt-id");
                request.setReservationId("YNJ-12345678");
                request.setPayload(objectMapper.readTree("{}"));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        null, "B", "BOOKING", null, request);

                // then
                assertThat(response.getBody().getEventId()).isEqualTo("body-evt-id");
            }
        }

        @Nested
        @DisplayName("헤더 eventId가 우선순위를 가지면")
        class Context_with_header_event_id_priority {

            @Test
            @DisplayName("헤더의 eventId를 사용한다")
            void it_uses_header_event_id() throws Exception {
                // given
                given(ingestionService.ingest(any(IngestRequest.class))).willReturn(true);

                IncomingEventRequest request = new IncomingEventRequest();
                request.setEventId("body-evt-id");
                request.setReservationId("YNJ-12345678");
                request.setPayload(objectMapper.readTree("{}"));

                // when
                ResponseEntity<EventResponse> response = controller.receiveEvent(
                        "header-evt-id", "A", "BOOKING", null, request);

                // then
                assertThat(response.getBody().getEventId()).isEqualTo("header-evt-id");
            }
        }
    }
}
