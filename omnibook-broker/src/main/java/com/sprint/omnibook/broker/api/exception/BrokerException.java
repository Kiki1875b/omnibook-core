package com.sprint.omnibook.broker.api.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Broker 모듈의 기본 예외.
 * 모든 도메인별 예외는 이 클래스를 상속받는다.
 */
@Getter
public class BrokerException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BrokerException(ErrorCode errorCode) {
        super(errorCode.getDefaultReason());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BrokerException(ErrorCode errorCode, String reason) {
        super(reason);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BrokerException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getDefaultReason());
        this.errorCode = errorCode;
        this.details = details;
    }

    public BrokerException(ErrorCode errorCode, String reason, Map<String, Object> details) {
        super(reason);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BrokerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultReason(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BrokerException(ErrorCode errorCode, String reason, Throwable cause) {
        super(reason, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
