package com.sprint.omnibook.broker.ingestion;

/**
 * 이벤트 수집 실패 메시지 상수.
 */
public final class IngestionErrorMessage {

    public static final String JSON_PARSE_FAILED_PREFIX = "JSON 파싱 실패: ";
    public static final String UNKNOWN_PLATFORM_PREFIX = "알 수 없는 플랫폼: ";
    public static final String TRANSLATOR_NOT_FOUND_PREFIX = "Translator 없음: ";
    public static final String PAYLOAD_SERIALIZATION_FAILED = "payload JSON 변환 실패";
    public static final String PROCESSING_FAILED = "처리 실패";
    public static final String SERIALIZATION_FAILED = "직렬화 실패";

    private IngestionErrorMessage() {
    }
}
