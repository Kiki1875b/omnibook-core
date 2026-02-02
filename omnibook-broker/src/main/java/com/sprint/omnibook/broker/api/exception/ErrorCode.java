package com.sprint.omnibook.broker.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 구조화된 에러 코드.
 * 도메인별로 그룹화되어 있으며, HTTP 상태 코드와 기본 메시지를 포함한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // === Events ===
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
    EVENT_PARSE_ERROR(HttpStatus.BAD_REQUEST, "이벤트 파싱에 실패했습니다."),
    INVALID_PLATFORM(HttpStatus.BAD_REQUEST, "지원하지 않는 플랫폼입니다."),
    INVALID_EVENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이벤트 타입입니다."),
    TRANSLATOR_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "해당 플랫폼의 Translator가 등록되지 않았습니다."),
    PAYLOAD_SERIALIZATION_FAILED(HttpStatus.BAD_REQUEST, "페이로드 직렬화에 실패했습니다."),
    PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 처리에 실패했습니다."),

    // === Reservations ===
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "중복된 예약입니다."),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),

    // === Inventory / Processing (FailureReason 매핑) ===
    UNKNOWN_ROOM(HttpStatus.BAD_REQUEST, "플랫폼 방 매핑을 찾을 수 없습니다."),
    NOT_AVAILABLE(HttpStatus.CONFLICT, "해당 기간에 재고가 없습니다."),
    ROOM_ALREADY_BOOKED(HttpStatus.CONFLICT, "이미 예약된 객실입니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "잘못된 날짜 범위입니다."),

    // === Idempotency ===
    IDEMPOTENCY_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "처리된 적 없는 이벤트입니다."),

    // === Reconciliation ===
    RECONCILIATION_PERIOD_TOO_LONG(HttpStatus.BAD_REQUEST, "조회 기간이 최대 허용 기간을 초과했습니다."),

    // === 공통 ===
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String defaultReason;
}
