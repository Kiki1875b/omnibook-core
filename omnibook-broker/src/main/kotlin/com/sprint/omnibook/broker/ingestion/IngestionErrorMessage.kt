package com.sprint.omnibook.broker.ingestion

/**
 * 이벤트 수집 실패 메시지 상수.
 */
object IngestionErrorMessage {
    const val JSON_PARSE_FAILED_PREFIX = "JSON 파싱 실패: "
    const val UNKNOWN_PLATFORM_PREFIX = "알 수 없는 플랫폼: "
    const val TRANSLATOR_NOT_FOUND_PREFIX = "Translator 없음: "
    const val PAYLOAD_SERIALIZATION_FAILED = "payload JSON 변환 실패"
    const val PROCESSING_FAILED = "처리 실패"
    const val SERIALIZATION_FAILED = "직렬화 실패"
}
