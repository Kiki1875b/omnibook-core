package com.sprint.omnibook.broker.translator;

/**
 * Payload 변환 실패 시 발생하는 예외.
 */
public class TranslationException extends RuntimeException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
