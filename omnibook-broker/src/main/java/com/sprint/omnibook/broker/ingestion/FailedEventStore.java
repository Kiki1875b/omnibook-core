package com.sprint.omnibook.broker.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 변환 실패 이벤트 임시 저장소.
 * 현재는 인메모리로 구현. 추후 RDB/Kafka 등으로 교체 가능.
 */
@Slf4j
@Component
public class FailedEventStore {

    private final ConcurrentLinkedQueue<FailedEvent> store = new ConcurrentLinkedQueue<>();

    public void save(FailedEvent event) {
        store.add(event);
        log.warn("[FailedEventStore] 저장됨: eventId={}, platform={}, error={}",
                event.getEventId(), event.getPlatform(), event.getErrorMessage());
    }

    public List<FailedEvent> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    public int count() {
        return store.size();
    }
}
