# Omnibook Core

다중 OTA(Online Travel Agency) 플랫폼으로부터 비정형 예약 데이터를 수신하여 정규화하고, 통합 재고 관리를 수행하는 브로커 시스템.

## 목적

- 비정형 데이터를 일관성 있게 받아들이고, 정규화된 형태로 저장
- 각 이벤트에 대해 성공/실패 판별
- 각 이벤트, 예약의 상태 추적

## 지원 플랫폼

| 플랫폼 | 특징 |
|--------|------|
| **Yanolja** | 한국 국내 OTA, 모바일 중심, 프로모션 중심 |
| **Airbnb** | 글로벌 호스트 기반 OTA, 숙박일 모델, 타임존 인식 |
| **YeogiEottae** | 한국 국내 OTA, 배치 지향, 정수 상태 코드 |

## 시스템 아키텍처

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Yanolja   │   │   Airbnb    │   │ YeogiEottae │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │ HTTP POST
                         ▼
              ┌─────────────────────┐
              │   EventController   │
              │   (thin layer)      │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │ EventIngestionService│
              └──────────┬──────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
   ┌────────────┐ ┌────────────┐ ┌────────────┐
   │  MongoDB   │ │ Translator │ │ Processing │
   │ (raw_events)│ │            │ │  Service   │
   └────────────┘ └────────────┘ └─────┬──────┘
                                       │
                                       ▼
                               ┌────────────┐
                               │ PostgreSQL │
                               │ (예약/재고) │
                               └────────────┘
```

## 데이터 저장소 역할

| 저장소 | 역할 | 특징 |
|--------|------|------|
| **MongoDB** (raw_events) | 원본 HTTP body | Append-only Source of Truth |
| **PostgreSQL** (reservation_event) | 정규화된 이벤트 | 감사/추적 |
| **PostgreSQL** (reservation) | 예약 상태 | 최종 상태 |
| **PostgreSQL** (inventory) | 날짜별 재고 | 예약 가능 여부 |

## 모듈 구조

```
omnibook-core/
├── omnibook-broker/          # 이벤트 브로커 (메인 서비스)
│   └── src/main/java/com/sprint/omnibook/broker/
│       ├── api/              # REST Controller
│       ├── domain/           # JPA 엔티티 & Repository
│       ├── event/            # 정규화된 이벤트 모델
│       ├── ingestion/        # 이벤트 수신 서비스
│       ├── persistence/      # MongoDB 저장
│       ├── processing/       # 예약 처리 서비스
│       ├── translator/       # 플랫폼별 Payload 변환
│       └── logging/          # AOP 로깅
│
└── omnibook-simulator/       # OTA 이벤트 시뮬레이터
    └── src/main/java/simulator/
        ├── platform/         # OTA 플랫폼 구현체
        ├── scenario/         # 테스트 시나리오
        ├── chaos/            # Chaos Engineering
        └── sender/           # HTTP 전송
```

## 도메인 모델

```
Property ───1:N─── Room ───1:N─── PlatformListing
                    │
                    ├───1:N─── Inventory ───N:1─── Reservation
                    │                                   │
                    └───────────1:N─────────────────────┘
                                                        │
                                               ┌────────┴────────┐
                                               │ ReservationEvent│
                                               └─────────────────┘
```

### 핵심 엔티티

| 엔티티 | 역할 |
|--------|------|
| **Property** | 숙박 시설 |
| **Room** | 예약 대상 객실 |
| **PlatformListing** | OTA 플랫폼 ↔ Room 매핑 |
| **Reservation** | 예약 (CONFIRMED, CANCELLED, COMPLETED, NOSHOW) |
| **Inventory** | 날짜별 재고 (AVAILABLE, BOOKED, BLOCKED) |
| **ReservationEventEntity** | 정규화된 이벤트 기록 |

## API

### 이벤트 수신

```http
POST /api/events
X-Platform: YANOLJA | AIRBNB | YEOGIEOTTAE
X-Event-Type: BOOKING | CANCELLATION
X-Correlation-Id: (optional)
Content-Type: application/json

{
  "eventId": "...",
  "reservationId": "...",
  "payload": { ... }
}
```

## 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.3
- **Build**: Gradle
- **Persistence**:
  - MongoDB 7 (원본 데이터)
  - PostgreSQL 16 (정규화 데이터)
- **Libraries**:
  - Spring Data JPA
  - Spring Data MongoDB
  - MapStruct (DTO 매핑)
  - Lombok

## 실행 방법

### 1. 인프라 실행

```bash
docker-compose up -d
```

- MongoDB: `localhost:27017`
- Mongo Express: `localhost:8081`
- PostgreSQL: `localhost:5432` (DB: omnibook)

### 2. 브로커 실행

```bash
./gradlew :omnibook-broker:bootRun
```

기본 포트: `8080`

### 3. 시뮬레이터 실행 (선택)

```bash
./gradlew :omnibook-simulator:bootRun
```

기본 포트: `8082`

## 설계 원칙

### 데이터 무손실

- Payload에 오류가 있어도 무조건 수신
- MongoDB에 원본 저장 후 파싱/처리
- 파싱 실패 시 FailedEvent로 별도 관리

### Source of Truth

- **MongoDB (raw_events)**: 이벤트 수신 사실 (Append-only, 업데이트 없음)
- **PostgreSQL (reservation_event)**: 무엇을 받았고 어떻게 처리되었는지
- **PostgreSQL (reservation)**: 예약의 최종 상태

### 정규화

플랫폼별 상이한 Payload를 `ReservationEvent`로 통합:

| 필드 | Yanolja | Airbnb | YeogiEottae |
|------|---------|--------|-------------|
| 예약 ID | reservationId | confirmationCode | orderId |
| 체크인 | checkInDate (yyyy-MM-dd) | checkIn (yyyy-MM-dd) | startDate (yyyyMMdd) |
| 게스트 | guestName | firstName + lastName | buyerName |
| 상태 | 한글 문자열 | 영문 문자열 | 정수 코드 |

## Phase 문서

- `Phase0/summary.md`: 설계 결정 및 구현 요약
- `Phase0/broker/entity.md`: 엔티티 명세서

## 가정 사항 (Phase 0)

- 브로커는 전 세계 모든 숙소 정보를 보유
- 이벤트 유형: BOOKING, CANCELLATION만 존재
- 플랫폼: Yanolja, Airbnb, YeogiEottae만 존재
- 각 플랫폼의 Payload 형태는 고정
- 이벤트는 중복 없이, 순서대로 도착
- 모든 요청은 시간을 두고 천천히 도착
