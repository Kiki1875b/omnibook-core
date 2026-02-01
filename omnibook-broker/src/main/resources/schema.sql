-- =====================================================
-- omnibook-broker PostgreSQL Schema
-- =====================================================

-- =====================================================
-- 테이블 삭제 (개발 환경: 매 실행 시 재생성)
-- FK 의존성 역순으로 삭제
-- =====================================================
DROP TABLE IF EXISTS failed_event CASCADE;
DROP TABLE IF EXISTS reservation_event CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS reservation CASCADE;
DROP TABLE IF EXISTS platform_listing CASCADE;
DROP TABLE IF EXISTS room CASCADE;
DROP TABLE IF EXISTS property CASCADE;

-- =====================================================
-- 테이블 생성
-- =====================================================

-- 숙소
CREATE TABLE property (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 방
CREATE TABLE room (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL REFERENCES property(id),
    name            VARCHAR(255) NOT NULL,
    room_type       VARCHAR(100),
    capacity        INT,
    status          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_property_id ON room(property_id);

-- OTA 플랫폼 방 매핑
CREATE TABLE platform_listing (
    id                      BIGSERIAL PRIMARY KEY,
    room_id                 BIGINT NOT NULL REFERENCES room(id),
    platform_type           VARCHAR(50) NOT NULL,
    platform_room_id        VARCHAR(255) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (room_id, platform_type),
    UNIQUE (platform_type, platform_room_id)
);

CREATE INDEX idx_platform_listing_room_id ON platform_listing(room_id);
CREATE INDEX idx_platform_listing_lookup ON platform_listing(platform_type, platform_room_id);

-- 예약
CREATE TABLE reservation (
    id                          BIGSERIAL PRIMARY KEY,
    room_id                     BIGINT NOT NULL REFERENCES room(id),
    platform_type               VARCHAR(50) NOT NULL,
    platform_reservation_id     VARCHAR(255) NOT NULL,
    check_in                    DATE NOT NULL,
    check_out                   DATE NOT NULL,
    guest_name                  VARCHAR(255),
    guest_phone                 VARCHAR(50),
    guest_email                 VARCHAR(255),
    total_amount                DECIMAL(15,2),
    status                      VARCHAR(50) NOT NULL,
    booked_at                   TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (platform_type, platform_reservation_id)
);

CREATE INDEX idx_reservation_room_id ON reservation(room_id);
CREATE INDEX idx_reservation_status ON reservation(status);
CREATE INDEX idx_reservation_dates ON reservation(room_id, check_in, check_out);

-- 날짜별 재고
CREATE TABLE inventory (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL REFERENCES room(id),
    date            DATE NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    reservation_id  BIGINT REFERENCES reservation(id),
    block_reason    VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (room_id, date)
);

CREATE INDEX idx_inventory_room_date ON inventory(room_id, date);
CREATE INDEX idx_inventory_status ON inventory(status);

-- 정규화된 이벤트
CREATE TABLE reservation_event (
    id                          BIGSERIAL PRIMARY KEY,
    event_id                    UUID NOT NULL UNIQUE,
    platform_type               VARCHAR(50) NOT NULL,
    platform_reservation_id     VARCHAR(255) NOT NULL,
    event_type                  VARCHAR(50) NOT NULL,
    room_id                     BIGINT REFERENCES room(id),
    reservation_id              BIGINT REFERENCES reservation(id),
    property_name               VARCHAR(255),
    property_address            VARCHAR(500),
    check_in                    DATE,
    check_out                   DATE,
    guest_name                  VARCHAR(255),
    guest_phone                 VARCHAR(50),
    guest_email                 VARCHAR(255),
    total_amount                DECIMAL(15,2),
    status                      VARCHAR(50),
    occurred_at                 TIMESTAMPTZ,
    received_at                 TIMESTAMPTZ NOT NULL,
    processed                   BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at                TIMESTAMPTZ,
    error_message               TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservation_event_platform ON reservation_event(platform_type, platform_reservation_id);
CREATE INDEX idx_reservation_event_processed ON reservation_event(processed);
CREATE INDEX idx_reservation_event_received ON reservation_event(received_at);

-- 실패 이벤트 (재처리용)
CREATE TABLE failed_event (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(255) NOT NULL,
    platform        VARCHAR(50) NOT NULL,
    event_type      VARCHAR(50),
    correlation_id  VARCHAR(255),
    reservation_id  VARCHAR(255),
    raw_payload     TEXT,
    error_message   TEXT,
    failed_at       TIMESTAMPTZ NOT NULL,
    retry_count     INT NOT NULL DEFAULT 0,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_failed_event_resolved ON failed_event(resolved);
CREATE INDEX idx_failed_event_platform ON failed_event(platform);
