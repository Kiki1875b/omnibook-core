package com.sprint.omnibook.broker.api.exception

/**
 * Broker 모듈의 기본 예외.
 * 모든 도메인별 예외는 이 클래스를 상속받는다.
 */
open class BrokerException : RuntimeException {

    val errorCode: ErrorCode
    val details: Map<String, Any>?

    constructor(errorCode: ErrorCode) : super(errorCode.defaultReason) {
        this.errorCode = errorCode
        this.details = null
    }

    constructor(errorCode: ErrorCode, reason: String) : super(reason) {
        this.errorCode = errorCode
        this.details = null
    }

    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode.defaultReason) {
        this.errorCode = errorCode
        this.details = details
    }

    constructor(errorCode: ErrorCode, reason: String, details: Map<String, Any>) : super(reason) {
        this.errorCode = errorCode
        this.details = details
    }

    constructor(errorCode: ErrorCode, cause: Throwable) : super(errorCode.defaultReason, cause) {
        this.errorCode = errorCode
        this.details = null
    }

    constructor(errorCode: ErrorCode, reason: String, cause: Throwable) : super(reason, cause) {
        this.errorCode = errorCode
        this.details = null
    }
}
