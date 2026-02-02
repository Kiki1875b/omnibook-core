package com.sprint.omnibook.broker.api.exception

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 대사 관련 예외.
 */
class ReconciliationException : BrokerException {

    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode, details)

    companion object {
        fun periodTooLong(from: LocalDate, to: LocalDate, maxDays: Int): ReconciliationException {
            val requestedDays = ChronoUnit.DAYS.between(from, to)
            return ReconciliationException(
                ErrorCode.RECONCILIATION_PERIOD_TOO_LONG,
                mapOf(
                    "from" to from.toString(),
                    "to" to to.toString(),
                    "requestedDays" to requestedDays,
                    "maxDays" to maxDays
                )
            )
        }
    }
}
