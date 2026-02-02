package com.sprint.omnibook.broker.processing

import com.sprint.omnibook.broker.domain.Reservation
import com.sprint.omnibook.broker.domain.Room

/**
 * 이벤트 처리 결과.
 */
class ProcessingResult private constructor(
    val success: Boolean,
    val failureReason: FailureReason?,
    val room: Room?,
    val reservation: Reservation?
) {
    val isSuccess: Boolean get() = success
    val isProcessed: Boolean get() = success

    companion object {
        /**
         * 처리 성공 결과 생성.
         */
        fun success(room: Room?, reservation: Reservation?): ProcessingResult =
            ProcessingResult(true, null, room, reservation)

        /**
         * 처리 실패 결과 생성.
         */
        fun failure(reason: FailureReason): ProcessingResult =
            ProcessingResult(false, reason, null, null)
    }
}
