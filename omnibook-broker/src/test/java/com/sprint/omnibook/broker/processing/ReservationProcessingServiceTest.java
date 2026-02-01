package com.sprint.omnibook.broker.processing;

import com.sprint.omnibook.broker.domain.*;
import com.sprint.omnibook.broker.domain.repository.*;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * ReservationProcessingService 테스트.
 *
 * 테스트 시나리오:
 * 1. BOOKING 성공 - 모든 날짜 가용
 * 2. BOOKING 실패 - UNKNOWN_ROOM
 * 3. BOOKING 실패 - NOT_AVAILABLE
 * 4. CANCELLATION 성공 - 기존 예약 있음
 * 5. CANCELLATION Silent Success - 예약 없음
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationProcessingService")
class ReservationProcessingServiceTest {

    @Mock
    private ReservationEventRepository reservationEventRepository;

    @Mock
    private PlatformListingRepository platformListingRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    private ReservationProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ReservationProcessingService(
                reservationEventRepository,
                platformListingRepository,
                inventoryRepository,
                reservationRepository
        );
    }

    // === 테스트 헬퍼 메서드 ===

    private ReservationEvent createBookingEvent() {
        return ReservationEvent.builder()
                .eventId(UUID.randomUUID())
                .platformType(PlatformType.YANOLJA)
                .platformReservationId("YNJ-12345")
                .eventType(EventType.BOOKING)
                .roomId("ROOM-001")
                .propertyName("테스트 숙소")
                .propertyAddress("서울시 강남구")
                .checkIn(LocalDate.of(2025, 3, 1))
                .checkOut(LocalDate.of(2025, 3, 3))
                .guestName("홍길동")
                .guestPhone("010-1234-5678")
                .guestEmail("hong@test.com")
                .totalAmount(BigDecimal.valueOf(150000))
                .status(com.sprint.omnibook.broker.event.ReservationStatus.CONFIRMED)
                .occurredAt(Instant.now())
                .receivedAt(Instant.now())
                .build();
    }

    private ReservationEvent createCancellationEvent() {
        return ReservationEvent.builder()
                .eventId(UUID.randomUUID())
                .platformType(PlatformType.YANOLJA)
                .platformReservationId("YNJ-12345")
                .eventType(EventType.CANCELLATION)
                .roomId("ROOM-001")
                .checkIn(LocalDate.of(2025, 3, 1))
                .checkOut(LocalDate.of(2025, 3, 3))
                .occurredAt(Instant.now())
                .receivedAt(Instant.now())
                .build();
    }

    private Property createProperty() {
        return Property.builder()
                .name("테스트 숙소")
                .address("서울시 강남구")
                .build();
    }

    private Room createRoom(Property property) {
        Room room = Room.builder()
                .name("디럭스룸")
                .roomType("DELUXE")
                .capacity(2)
                .build();
        room.setProperty(property);
        return room;
    }

    private PlatformListing createPlatformListing(Room room) {
        PlatformListing listing = PlatformListing.builder()
                .platformType(PlatformType.YANOLJA)
                .platformRoomId("ROOM-001")
                .build();
        listing.setRoom(room);
        return listing;
    }

    private Reservation createReservation(Room room) {
        ReservationEvent event = createBookingEvent();
        return Reservation.book(room, event);
    }

    // === 테스트 케이스 ===

    @Nested
    @DisplayName("process 메서드는")
    class Describe_process {

        @Nested
        @DisplayName("ReservationEventEntity를")
        class Context_reservation_event_entity {

            @Test
            @DisplayName("항상 먼저 저장한다")
            void it_always_saves_event_entity_first() {
                // given
                ReservationEvent event = createBookingEvent();

                // platformListing이 없어서 실패하더라도 이벤트는 저장되어야 함
                given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(Optional.empty());
                given(reservationEventRepository.save(any()))
                        .willAnswer(inv -> inv.getArgument(0));

                // when
                service.process(event);

                // then - 최소 한 번은 저장되어야 함 (처음 저장 + markFailed 후 저장)
                ArgumentCaptor<ReservationEventEntity> captor =
                        ArgumentCaptor.forClass(ReservationEventEntity.class);
                then(reservationEventRepository).should(atLeastOnce()).save(captor.capture());

                // 첫 번째 저장된 엔티티 검증
                ReservationEventEntity firstSaved = captor.getAllValues().get(0);
                assertThat(firstSaved.getEventId()).isEqualTo(event.getEventId());
                assertThat(firstSaved.getPlatformType()).isEqualTo(PlatformType.YANOLJA);
                assertThat(firstSaved.getEventType()).isEqualTo(EventType.BOOKING);
            }
        }

        @Nested
        @DisplayName("BOOKING 이벤트가 주어지고")
        class Context_with_booking_event {

            @Nested
            @DisplayName("모든 조건이 충족되면")
            class Context_when_all_conditions_met {

                @Test
                @DisplayName("Reservation을 생성하고 Inventory를 BOOKED 처리한다")
                void it_creates_reservation_and_books_inventory() {
                    // given
                    ReservationEvent event = createBookingEvent();
                    Property property = createProperty();
                    Room room = createRoom(property);
                    PlatformListing platformListing = createPlatformListing(room);

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(
                            PlatformType.YANOLJA, "ROOM-001"))
                            .willReturn(Optional.of(platformListing));
                    // 해당 기간에 Inventory가 없음 → 가용
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(
                            any(), any(), any(), any()))
                            .willReturn(List.of());
                    given(reservationRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(inventoryRepository.findByRoomAndDate(any(), any()))
                            .willReturn(Optional.empty());
                    given(inventoryRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFailureReason()).isNull();

                    // Reservation 저장 검증
                    ArgumentCaptor<Reservation> reservationCaptor =
                            ArgumentCaptor.forClass(Reservation.class);
                    then(reservationRepository).should().save(reservationCaptor.capture());

                    Reservation savedReservation = reservationCaptor.getValue();
                    assertThat(savedReservation.getPlatformReservationId()).isEqualTo("YNJ-12345");
                    assertThat(savedReservation.getCheckIn()).isEqualTo(LocalDate.of(2025, 3, 1));
                    assertThat(savedReservation.getCheckOut()).isEqualTo(LocalDate.of(2025, 3, 3));
                    assertThat(savedReservation.getGuestName()).isEqualTo("홍길동");
                    assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

                    // Inventory 저장 검증 (checkIn ~ checkOut-1 = 3/1, 3/2 -> 2일)
                    then(inventoryRepository).should(times(2)).save(any(Inventory.class));
                }

                @Test
                @DisplayName("기존 Inventory가 AVAILABLE이면 BOOKED로 변경한다")
                void it_books_existing_available_inventory() {
                    // given
                    ReservationEvent event = createBookingEvent();
                    Property property = createProperty();
                    Room room = createRoom(property);
                    PlatformListing platformListing = createPlatformListing(room);

                    Inventory existingInventory = Inventory.createBooked(room, LocalDate.of(2025, 3, 1));

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.of(platformListing));
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                            .willReturn(List.of());
                    given(reservationRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    // 첫 번째 날짜는 기존 Inventory 존재, 두 번째 날짜는 없음
                    given(inventoryRepository.findByRoomAndDate(eq(room), eq(LocalDate.of(2025, 3, 1))))
                            .willReturn(Optional.of(existingInventory));
                    given(inventoryRepository.findByRoomAndDate(eq(room), eq(LocalDate.of(2025, 3, 2))))
                            .willReturn(Optional.empty());
                    given(inventoryRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(existingInventory.getStatus()).isEqualTo(InventoryStatus.BOOKED);
                }
            }

            @Nested
            @DisplayName("PlatformListing이 없으면")
            class Context_when_platform_listing_not_found {

                @Test
                @DisplayName("UNKNOWN_ROOM 실패를 반환한다")
                void it_returns_unknown_room_failure() {
                    // given
                    ReservationEvent event = createBookingEvent();

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.empty());

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getFailureReason()).isEqualTo(FailureReason.UNKNOWN_ROOM);

                    // Reservation 저장하지 않음
                    then(reservationRepository).should(never()).save(any());
                }

                @Test
                @DisplayName("ReservationEventEntity를 markFailed 처리한다")
                void it_marks_event_entity_as_failed() {
                    // given
                    ReservationEvent event = createBookingEvent();

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.empty());

                    // when
                    service.process(event);

                    // then - save가 두 번 호출됨 (처음 저장 + markFailed 후 저장)
                    ArgumentCaptor<ReservationEventEntity> captor =
                            ArgumentCaptor.forClass(ReservationEventEntity.class);
                    then(reservationEventRepository).should(times(2)).save(captor.capture());

                    ReservationEventEntity finalSaved = captor.getAllValues().get(1);
                    assertThat(finalSaved.isProcessed()).isFalse();
                    assertThat(finalSaved.getErrorMessage()).contains("UNKNOWN_ROOM");
                }
            }

            @Nested
            @DisplayName("해당 기간에 불가용 Inventory가 있으면")
            class Context_when_inventory_not_available {

                @Test
                @DisplayName("NOT_AVAILABLE 실패를 반환한다")
                void it_returns_not_available_failure() {
                    // given
                    ReservationEvent event = createBookingEvent();
                    Property property = createProperty();
                    Room room = createRoom(property);
                    PlatformListing platformListing = createPlatformListing(room);

                    Inventory bookedInventory = Inventory.createBooked(room, LocalDate.of(2025, 3, 1));
                    bookedInventory.book(null); // BOOKED 상태로 변경

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.of(platformListing));
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                            .willReturn(List.of(bookedInventory));

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isFalse();
                    assertThat(result.getFailureReason()).isEqualTo(FailureReason.NOT_AVAILABLE);

                    // Reservation 저장하지 않음
                    then(reservationRepository).should(never()).save(any());
                }
            }
        }

        @Nested
        @DisplayName("CANCELLATION 이벤트가 주어지고")
        class Context_with_cancellation_event {

            @Nested
            @DisplayName("기존 예약이 있으면")
            class Context_when_reservation_exists {

                @Test
                @DisplayName("Reservation을 CANCELLED로 변경하고 Inventory를 release한다")
                void it_cancels_reservation_and_releases_inventory() {
                    // given
                    ReservationEvent event = createCancellationEvent();
                    Property property = createProperty();
                    Room room = createRoom(property);
                    PlatformListing platformListing = createPlatformListing(room);
                    Reservation existingReservation = createReservation(room);

                    Inventory inventory1 = Inventory.createBooked(room, LocalDate.of(2025, 3, 1));
                    inventory1.book(existingReservation);

                    Inventory inventory2 = Inventory.createBooked(room, LocalDate.of(2025, 3, 2));
                    inventory2.book(existingReservation);

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.of(platformListing));
                    given(reservationRepository.findByPlatformTypeAndPlatformReservationId(
                            PlatformType.YANOLJA, "YNJ-12345"))
                            .willReturn(Optional.of(existingReservation));
                    given(inventoryRepository.findByRoomAndDateBetween(
                            eq(room),
                            eq(LocalDate.of(2025, 3, 1)),
                            eq(LocalDate.of(2025, 3, 2))))
                            .willReturn(List.of(inventory1, inventory2));

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(existingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
                    assertThat(inventory1.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
                    assertThat(inventory2.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
                }
            }

            @Nested
            @DisplayName("기존 예약이 없으면")
            class Context_when_reservation_not_exists {

                @Test
                @DisplayName("Silent Success - 에러 없이 성공 처리한다")
                void it_silently_succeeds() {
                    // given
                    ReservationEvent event = createCancellationEvent();
                    Property property = createProperty();
                    Room room = createRoom(property);
                    PlatformListing platformListing = createPlatformListing(room);

                    given(reservationEventRepository.save(any()))
                            .willAnswer(inv -> inv.getArgument(0));
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                            .willReturn(Optional.of(platformListing));
                    given(reservationRepository.findByPlatformTypeAndPlatformReservationId(any(), any()))
                            .willReturn(Optional.empty());

                    // when
                    ProcessingResult result = service.process(event);

                    // then
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFailureReason()).isNull();
                }
            }
        }

        @Nested
        @DisplayName("처리 성공 시")
        class Context_when_processing_succeeds {

            @Test
            @DisplayName("ReservationEventEntity를 markProcessed 처리한다")
            void it_marks_event_entity_as_processed() {
                // given
                ReservationEvent event = createBookingEvent();
                Property property = createProperty();
                Room room = createRoom(property);
                PlatformListing platformListing = createPlatformListing(room);

                given(reservationEventRepository.save(any()))
                        .willAnswer(inv -> inv.getArgument(0));
                given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(Optional.of(platformListing));
                given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                        .willReturn(List.of());
                given(reservationRepository.save(any()))
                        .willAnswer(inv -> inv.getArgument(0));
                given(inventoryRepository.findByRoomAndDate(any(), any()))
                        .willReturn(Optional.empty());
                given(inventoryRepository.save(any()))
                        .willAnswer(inv -> inv.getArgument(0));

                // when
                service.process(event);

                // then - save가 두 번 호출됨 (처음 저장 + markProcessed 후 저장)
                ArgumentCaptor<ReservationEventEntity> captor =
                        ArgumentCaptor.forClass(ReservationEventEntity.class);
                then(reservationEventRepository).should(times(2)).save(captor.capture());

                ReservationEventEntity finalSaved = captor.getAllValues().get(1);
                assertThat(finalSaved.isProcessed()).isTrue();
                assertThat(finalSaved.getRoom()).isEqualTo(room);
            }
        }
    }
}
