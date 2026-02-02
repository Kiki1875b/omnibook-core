package com.sprint.omnibook.broker.translator

/**
 * Payload 변환 실패 시 발생하는 예외.
 */
class TranslationException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
