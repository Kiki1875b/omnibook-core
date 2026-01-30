package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.event.ReservationStatus;
import com.sprint.omnibook.broker.persistence.RawEventService;
import com.sprint.omnibook.broker.processing.ProcessingResult;
import com.sprint.omnibook.broker.processing.ReservationProcessingService;
import com.sprint.omnibook.broker.translator.PayloadTranslator;
import com.sprint.omnibook.broker.translator.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventIngestionService")
class EventIngestionServiceTest {

    @Mock
    private RawEventService rawEventService;

    @Mock
    private PayloadTranslator yanoljaTranslator;

    @Mock
    private PayloadTranslator airbnbTranslator;

    @Mock
    private PayloadTranslator yeogieottaeTranslator;

    @Mock
    private ReservationProcessingService reservationProcessingService;

    private FailedEventStore failedEventStore;
    private ObjectMapper objectMapper;
    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        failedEventStore = new FailedEventStore();

        Map<PlatformType, PayloadTranslator> translators = new EnumMap<>(PlatformType.class);
        translators.put(PlatformType.YANOLJA, yanoljaTranslator);
        translators.put(PlatformType.AIRBNB, airbnbTranslator);
        translators.put(PlatformType.YEOGIEOTTAE, yeogieottaeTranslator);

        service = new EventIngestionService(
                rawEventService,
                translators,
                failedEventStore,
                objectMapper,
                reservationProcessingService
        );
    }

    @Nested
    @DisplayName("ingest 메서드는")
    class Describe_ingest {

        @Nested
        @DisplayName("유효한 요청이 주어지면")
        class Context_with_valid_request {

            @ParameterizedTest
            @CsvSource({
                    "A, YANOLJA",
                    "B, AIRBNB",
                    "C, YEOGIEOTTAE",
                    "YANOLJA, YANOLJA",
                    "AIRBNB, AIRBNB",
                    "YEOGIEOTTAE, YEOGIEOTTAE"
            })
            @DisplayName("플랫폼 헤더를 올바르게 매핑한다")
            void it_maps_platform_correctly(String header, PlatformType expected) throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"reservationId\": \"TEST-001\"}");
                IngestRequest request = new IngestRequest(
                        "evt-1", header, "BOOKING", "corr-1", "res-1", payload
                );

                PayloadTranslator expectedTranslator = switch (expected) {
                    case YANOLJA -> yanoljaTranslator;
                    case AIRBNB -> airbnbTranslator;
                    case YEOGIEOTTAE -> yeogieottaeTranslator;
                };

                given(expectedTranslator.translate(any(), any())).willReturn(createMockEvent());
                given(reservationProcessingService.process(any()))
                        .willReturn(ProcessingResult.success(null, null));

                // when
                boolean result = service.ingest(request);

                // then
                assertThat(result).isTrue();
                then(expectedTranslator).should().translate(any(), eq(EventType.BOOKING));
            }

            @ParameterizedTest
            @CsvSource({
                    "BOOKING, BOOKING",
                    "CANCEL, CANCELLATION",
                    "CANCELLATION, CANCELLATION"
            })
            @DisplayName("이벤트 타입을 올바르게 매핑한다")
            void it_maps_event_type_correctly(String header, EventType expected) throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"test\": true}");
                IngestRequest request = new IngestRequest(
                        "evt-1", "A", header, "corr-1", "res-1", payload
                );

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent());
                given(reservationProcessingService.process(any()))
                        .willReturn(ProcessingResult.success(null, null));

                // when
                service.ingest(request);

                // then
                then(yanoljaTranslator).should().translate(any(), eq(expected));
            }
        }

        @Nested
        @DisplayName("알 수 없는 플랫폼이 주어지면")
        class Context_with_unknown_platform {

            @Test
            @DisplayName("실패를 반환하고 원본을 저장한다")
            void it_saves_failed_event() throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"test\": true}");
                IngestRequest request = new IngestRequest(
                        "evt-1", "UNKNOWN", "BOOKING", "corr-1", "res-1", payload
                );

                // when
                boolean result = service.ingest(request);

                // then
                assertThat(result).isFalse();
                assertThat(failedEventStore.count()).isEqualTo(1);
                assertThat(failedEventStore.findAll().get(0).getErrorMessage())
                        .contains("알 수 없는 플랫폼");
            }
        }

        @Nested
        @DisplayName("Translator가 예외를 던지면")
        class Context_when_translator_throws {

            @Test
            @DisplayName("실패를 반환하고 원본을 저장한다")
            void it_saves_failed_event() throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"invalid\": \"data\"}");
                IngestRequest request = new IngestRequest(
                        "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                );

                given(yanoljaTranslator.translate(any(), any()))
                        .willThrow(new TranslationException("파싱 실패"));

                // when
                boolean result = service.ingest(request);

                // then
                assertThat(result).isFalse();
                assertThat(failedEventStore.count()).isEqualTo(1);
                FailedEvent failed = failedEventStore.findAll().get(0);
                assertThat(failed.getEventId()).isEqualTo("evt-1");
                assertThat(failed.getErrorMessage()).isEqualTo("파싱 실패");
            }
        }

        @Nested
        @DisplayName("ReservationProcessingService 호출 시")
        class Context_when_calling_processing_service {

            @Test
            @DisplayName("처리 성공 시 true를 반환한다")
            void it_returns_true_on_success() throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"test\": true}");
                IngestRequest request = new IngestRequest(
                        "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                );

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent());
                given(reservationProcessingService.process(any()))
                        .willReturn(ProcessingResult.success(null, null));

                // when
                boolean result = service.ingest(request);

                // then
                assertThat(result).isTrue();
                then(reservationProcessingService).should().process(any());
            }

            @Test
            @DisplayName("처리 실패 시 false를 반환한다")
            void it_returns_false_on_failure() throws Exception {
                // given
                JsonNode payload = objectMapper.readTree("{\"test\": true}");
                IngestRequest request = new IngestRequest(
                        "evt-1", "A", "BOOKING", "corr-1", "res-1", payload
                );

                given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent());
                given(reservationProcessingService.process(any()))
                        .willReturn(ProcessingResult.failure(
                                com.sprint.omnibook.broker.processing.FailureReason.UNKNOWN_PROPERTY));

                // when
                boolean result = service.ingest(request);

                // then
                assertThat(result).isFalse();
                // FailedEventStore에는 저장하지 않음 (ReservationEventEntity에 기록됨)
                assertThat(failedEventStore.count()).isEqualTo(0);
            }
        }
    }

    @Nested
    @DisplayName("process 메서드는")
    class Describe_process {

        @Test
        @DisplayName("RawEventService를 호출하고 결과를 반환한다")
        void it_calls_raw_event_service_and_returns_result() throws Exception {
            // given
            String rawBody = """
                    {
                        "eventId": "evt-123",
                        "reservationId": "YNJ-12345678",
                        "payload": {"roomId": "R-101"}
                    }
                    """;
            EventHeaders headers = new EventHeaders("evt-123", "A", "BOOKING", "corr-456");

            JsonNode payload = objectMapper.readTree("{\"roomId\": \"R-101\"}");
            IngestRequest ingestRequest = new IngestRequest(
                    "evt-123", "A", "BOOKING", "corr-456", "YNJ-12345678", payload
            );

            given(rawEventService.receiveAndStore(rawBody, headers)).willReturn(ingestRequest);
            given(yanoljaTranslator.translate(any(), any())).willReturn(createMockEvent());
            given(reservationProcessingService.process(any()))
                    .willReturn(ProcessingResult.success(null, null));

            // when
            IngestionResult result = service.process(rawBody, headers);

            // then
            assertThat(result.eventId()).isEqualTo("evt-123");
            assertThat(result.success()).isTrue();
            then(rawEventService).should().receiveAndStore(rawBody, headers);
        }
    }

    private ReservationEvent createMockEvent() {
        return ReservationEvent.builder()
                .eventId(UUID.randomUUID())
                .platformType(PlatformType.YANOLJA)
                .status(ReservationStatus.CONFIRMED)
                .receivedAt(Instant.now())
                .build();
    }
}
