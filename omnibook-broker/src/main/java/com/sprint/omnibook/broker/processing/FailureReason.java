package com.sprint.omnibook.broker.processing;

/**
 * 이벤트 처리 실패 원인.
 */
public enum FailureReason {
    /** 플랫폼 숙소 매핑을 찾을 수 없음 */
    UNKNOWN_PROPERTY,

    /** 플랫폼 방 매핑을 찾을 수 없음 */
    UNKNOWN_ROOM,

    /** 해당 기간 재고 불가 */
    NOT_AVAILABLE
}
