package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.FailedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedEventRepository extends JpaRepository<FailedEventEntity, Long> {

    /**
     * 미해결 실패 이벤트 조회.
     */
    List<FailedEventEntity> findByResolvedFalseOrderByFailedAtAsc();

    /**
     * 재시도 횟수 기준 미해결 이벤트 조회.
     */
    List<FailedEventEntity> findByResolvedFalseAndRetryCountLessThanOrderByFailedAtAsc(int maxRetryCount);
}
