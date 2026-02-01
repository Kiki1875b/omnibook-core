package com.sprint.omnibook.broker.processing;

import com.sprint.omnibook.broker.domain.Reservation;
import com.sprint.omnibook.broker.domain.Room;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이벤트 처리 결과.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessingResult {

    private final boolean success;
    private final FailureReason failureReason;
    private final Room room;
    private final Reservation reservation;

    /**
     * 처리 성공 결과 생성.
     */
    public static ProcessingResult success(Room room, Reservation reservation) {
        return new ProcessingResult(true, null, room, reservation);
    }

    /**
     * 처리 실패 결과 생성.
     */
    public static ProcessingResult failure(FailureReason reason) {
        return new ProcessingResult(false, reason, null, null);
    }
}
