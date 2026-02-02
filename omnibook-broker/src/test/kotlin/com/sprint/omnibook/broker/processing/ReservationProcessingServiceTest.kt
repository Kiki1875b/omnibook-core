package com.sprint.omnibook.broker.processing

import com.sprint.omnibook.broker.domain.*
import com.sprint.omnibook.broker.domain.repository.*
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

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
@ExtendWith(MockitoExtension::class)
@DisplayName("ReservationProcessingService")
class ReservationProcessingServiceTest {

    @Mock
    private lateinit var reservationEventRepository: ReservationEventRepository

    @Mock
    private lateinit var platformListingRepository: PlatformListingRepository

    @Mock
    private lateinit var inventoryRepository: InventoryRepository

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    private lateinit var service: ReservationProcessingService

    @BeforeEach
    fun setUp() {
        service = ReservationProcessingService(
            reservationEventRepository,
            platformListingRepository,
            inventoryRepository,
            reservationRepository
        )
    }

    // === 테스트 헬퍼 메서드 ===

    private fun createBookingEvent(): ReservationEvent {
        return ReservationEvent(
            eventId = UUID.randomUUID(),
            platformType = PlatformType.YANOLJA,
            platformReservationId = "YNJ-12345",
            eventType = EventType.BOOKING,
            roomId = "ROOM-001",
            propertyName = "테스트 숙소",
            propertyAddress = "서울시 강남구",
            checkIn = LocalDate.of(2025, 3, 1),
            checkOut = LocalDate.of(2025, 3, 3),
            guestName = "홍길동",
            guestPhone = "010-1234-5678",
            guestEmail = "hong@test.com",
            totalAmount = BigDecimal.valueOf(150000),
            status = com.sprint.omnibook.broker.event.ReservationStatus.CONFIRMED,
            occurredAt = Instant.now(),
            receivedAt = Instant.now()
        )
    }

    private fun createCancellationEvent(): ReservationEvent {
        return ReservationEvent(
            eventId = UUID.randomUUID(),
            platformType = PlatformType.YANOLJA,
            platformReservationId = "YNJ-12345",
            eventType = EventType.CANCELLATION,
            roomId = "ROOM-001",
            checkIn = LocalDate.of(2025, 3, 1),
            checkOut = LocalDate.of(2025, 3, 3),
            status = com.sprint.omnibook.broker.event.ReservationStatus.CANCELLED,
            occurredAt = Instant.now(),
            receivedAt = Instant.now()
        )
    }

    private fun createProperty(): Property {
        return Property(
            name = "테스트 숙소",
            address = "서울시 강남구"
        )
    }

    private fun createRoom(property: Property): Room {
        val room = Room(
            name = "디럭스룸",
            roomType = "DELUXE",
            capacity = 2
        )
        property.addRoom(room)
        return room
    }

    private fun createPlatformListing(room: Room): PlatformListing {
        val listing = PlatformListing(
            platformType = PlatformType.YANOLJA,
            platformRoomId = "ROOM-001"
        )
        room.addPlatformListing(listing)
        return listing
    }

    private fun createReservation(room: Room): Reservation {
        val event = createBookingEvent()
        return Reservation.book(room, event)
    }

    // === 테스트 케이스 ===

    @Nested
    @DisplayName("process 메서드는")
    inner class Describe_process {

        @Nested
        @DisplayName("ReservationEventEntity를")
        inner class Context_reservation_event_entity {

            @Test
            @DisplayName("항상 먼저 저장한다")
            fun it_always_saves_event_entity_first() {
                // given
                val event = createBookingEvent()

                // platformListing이 없어서 실패하더라도 이벤트는 저장되어야 함
                given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                    .willReturn(null)
                given(reservationEventRepository.save(any<ReservationEventEntity>()))
                    .willAnswer { it.arguments[0] }

                // when
                service.process(event)

                // then - 최소 한 번은 저장되어야 함 (처음 저장 + markFailed 후 저장)
                val captor = ArgumentCaptor.forClass(ReservationEventEntity::class.java)
                verify(reservationEventRepository, atLeastOnce()).save(captor.capture())

                // 첫 번째 저장된 엔티티 검증
                val firstSaved = captor.allValues[0]
                assertThat(firstSaved.eventId).isEqualTo(event.eventId)
                assertThat(firstSaved.platformType).isEqualTo(PlatformType.YANOLJA)
                assertThat(firstSaved.eventType).isEqualTo(EventType.BOOKING)
            }
        }

        @Nested
        @DisplayName("BOOKING 이벤트가 주어지고")
        inner class Context_with_booking_event {

            @Nested
            @DisplayName("모든 조건이 충족되면")
            inner class Context_when_all_conditions_met {

                @Test
                @DisplayName("Reservation을 생성하고 Inventory를 BOOKED 처리한다")
                fun it_creates_reservation_and_books_inventory() {
                    // given
                    val event = createBookingEvent()
                    val property = createProperty()
                    val room = createRoom(property)
                    val platformListing = createPlatformListing(room)

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(
                        PlatformType.YANOLJA, "ROOM-001"))
                        .willReturn(platformListing)
                    // 해당 기간에 Inventory가 없음 → 가용
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                        .willReturn(emptyList())
                    given(reservationRepository.save(any<Reservation>()))
                        .willAnswer { it.arguments[0] }
                    given(inventoryRepository.findByRoomAndDate(any(), any()))
                        .willReturn(null)
                    given(inventoryRepository.save(any<Inventory>()))
                        .willAnswer { it.arguments[0] }

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isTrue()
                    assertThat(result.failureReason).isNull()

                    // Reservation 저장 검증
                    val reservationCaptor = ArgumentCaptor.forClass(Reservation::class.java)
                    verify(reservationRepository).save(reservationCaptor.capture())

                    val savedReservation = reservationCaptor.value
                    assertThat(savedReservation.platformReservationId).isEqualTo("YNJ-12345")
                    assertThat(savedReservation.checkIn).isEqualTo(LocalDate.of(2025, 3, 1))
                    assertThat(savedReservation.checkOut).isEqualTo(LocalDate.of(2025, 3, 3))
                    assertThat(savedReservation.guestName).isEqualTo("홍길동")
                    assertThat(savedReservation.status).isEqualTo(ReservationStatus.CONFIRMED)

                    // Inventory 저장 검증 (checkIn ~ checkOut-1 = 3/1, 3/2 -> 2일)
                    verify(inventoryRepository, times(2)).save(any<Inventory>())
                }

                @Test
                @DisplayName("기존 Inventory가 AVAILABLE이면 BOOKED로 변경한다")
                fun it_books_existing_available_inventory() {
                    // given
                    val event = createBookingEvent()
                    val property = createProperty()
                    val room = createRoom(property)
                    val platformListing = createPlatformListing(room)

                    val existingInventory = Inventory.createBooked(room, LocalDate.of(2025, 3, 1))

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(platformListing)
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                        .willReturn(emptyList())
                    given(reservationRepository.save(any<Reservation>()))
                        .willAnswer { it.arguments[0] }
                    // 첫 번째 날짜는 기존 Inventory 존재, 두 번째 날짜는 없음
                    given(inventoryRepository.findByRoomAndDate(eq(room), eq(LocalDate.of(2025, 3, 1))))
                        .willReturn(existingInventory)
                    given(inventoryRepository.findByRoomAndDate(eq(room), eq(LocalDate.of(2025, 3, 2))))
                        .willReturn(null)
                    given(inventoryRepository.save(any<Inventory>()))
                        .willAnswer { it.arguments[0] }

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isTrue()
                    assertThat(existingInventory.status).isEqualTo(InventoryStatus.BOOKED)
                }
            }

            @Nested
            @DisplayName("PlatformListing이 없으면")
            inner class Context_when_platform_listing_not_found {

                @Test
                @DisplayName("UNKNOWN_ROOM 실패를 반환한다")
                fun it_returns_unknown_room_failure() {
                    // given
                    val event = createBookingEvent()

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(null)

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isFalse()
                    assertThat(result.failureReason).isEqualTo(FailureReason.UNKNOWN_ROOM)

                    // Reservation 저장하지 않음
                    verify(reservationRepository, never()).save(any<Reservation>())
                }

                @Test
                @DisplayName("ReservationEventEntity를 markFailed 처리한다")
                fun it_marks_event_entity_as_failed() {
                    // given
                    val event = createBookingEvent()

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(null)

                    // when
                    service.process(event)

                    // then - save가 두 번 호출됨 (처음 저장 + markFailed 후 저장)
                    val captor = ArgumentCaptor.forClass(ReservationEventEntity::class.java)
                    verify(reservationEventRepository, times(2)).save(captor.capture())

                    val finalSaved = captor.allValues[1]
                    assertThat(finalSaved.isProcessed).isFalse()
                    assertThat(finalSaved.errorMessage).contains("UNKNOWN_ROOM")
                }
            }

            @Nested
            @DisplayName("해당 기간에 불가용 Inventory가 있으면")
            inner class Context_when_inventory_not_available {

                @Test
                @DisplayName("NOT_AVAILABLE 실패를 반환한다")
                fun it_returns_not_available_failure() {
                    // given
                    val event = createBookingEvent()
                    val property = createProperty()
                    val room = createRoom(property)
                    val platformListing = createPlatformListing(room)

                    val bookedInventory = Inventory.createBooked(room, LocalDate.of(2025, 3, 1))
                    bookedInventory.book(null) // BOOKED 상태로 변경

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(platformListing)
                    given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                        .willReturn(listOf(bookedInventory))

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isFalse()
                    assertThat(result.failureReason).isEqualTo(FailureReason.NOT_AVAILABLE)

                    // Reservation 저장하지 않음
                    verify(reservationRepository, never()).save(any<Reservation>())
                }
            }
        }

        @Nested
        @DisplayName("CANCELLATION 이벤트가 주어지고")
        inner class Context_with_cancellation_event {

            @Nested
            @DisplayName("기존 예약이 있으면")
            inner class Context_when_reservation_exists {

                @Test
                @DisplayName("Reservation을 CANCELLED로 변경하고 Inventory를 release한다")
                fun it_cancels_reservation_and_releases_inventory() {
                    // given
                    val event = createCancellationEvent()
                    val property = createProperty()
                    val room = createRoom(property)
                    val platformListing = createPlatformListing(room)
                    val existingReservation = createReservation(room)

                    val inventory1 = Inventory.createBooked(room, LocalDate.of(2025, 3, 1))
                    inventory1.book(existingReservation)

                    val inventory2 = Inventory.createBooked(room, LocalDate.of(2025, 3, 2))
                    inventory2.book(existingReservation)

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(platformListing)
                    given(reservationRepository.findByPlatformTypeAndPlatformReservationId(
                        PlatformType.YANOLJA, "YNJ-12345"))
                        .willReturn(existingReservation)
                    given(inventoryRepository.findByRoomAndDateBetween(
                        eq(room),
                        eq(LocalDate.of(2025, 3, 1)),
                        eq(LocalDate.of(2025, 3, 2))))
                        .willReturn(listOf(inventory1, inventory2))

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isTrue()
                    assertThat(existingReservation.status).isEqualTo(ReservationStatus.CANCELLED)
                    assertThat(inventory1.status).isEqualTo(InventoryStatus.AVAILABLE)
                    assertThat(inventory2.status).isEqualTo(InventoryStatus.AVAILABLE)
                }
            }

            @Nested
            @DisplayName("기존 예약이 없으면")
            inner class Context_when_reservation_not_exists {

                @Test
                @DisplayName("Silent Success - 에러 없이 성공 처리한다")
                fun it_silently_succeeds() {
                    // given
                    val event = createCancellationEvent()
                    val property = createProperty()
                    val room = createRoom(property)
                    val platformListing = createPlatformListing(room)

                    given(reservationEventRepository.save(any<ReservationEventEntity>()))
                        .willAnswer { it.arguments[0] }
                    given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                        .willReturn(platformListing)
                    given(reservationRepository.findByPlatformTypeAndPlatformReservationId(any(), any()))
                        .willReturn(null)

                    // when
                    val result = service.process(event)

                    // then
                    assertThat(result.isSuccess).isTrue()
                    assertThat(result.failureReason).isNull()
                }
            }
        }

        @Nested
        @DisplayName("처리 성공 시")
        inner class Context_when_processing_succeeds {

            @Test
            @DisplayName("ReservationEventEntity를 markProcessed 처리한다")
            fun it_marks_event_entity_as_processed() {
                // given
                val event = createBookingEvent()
                val property = createProperty()
                val room = createRoom(property)
                val platformListing = createPlatformListing(room)

                given(reservationEventRepository.save(any<ReservationEventEntity>()))
                    .willAnswer { it.arguments[0] }
                given(platformListingRepository.findByPlatformTypeAndPlatformRoomId(any(), any()))
                    .willReturn(platformListing)
                given(inventoryRepository.findUnavailableByRoomAndDateRange(any(), any(), any(), any()))
                    .willReturn(emptyList())
                given(reservationRepository.save(any<Reservation>()))
                    .willAnswer { it.arguments[0] }
                given(inventoryRepository.findByRoomAndDate(any(), any()))
                    .willReturn(null)
                given(inventoryRepository.save(any<Inventory>()))
                    .willAnswer { it.arguments[0] }

                // when
                service.process(event)

                // then - save가 두 번 호출됨 (처음 저장 + markProcessed 후 저장)
                val captor = ArgumentCaptor.forClass(ReservationEventEntity::class.java)
                verify(reservationEventRepository, times(2)).save(captor.capture())

                val finalSaved = captor.allValues[1]
                assertThat(finalSaved.isProcessed).isTrue()
                assertThat(finalSaved.room).isEqualTo(room)
            }
        }
    }
}
