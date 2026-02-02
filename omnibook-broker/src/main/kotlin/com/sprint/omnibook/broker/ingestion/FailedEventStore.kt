package com.sprint.omnibook.broker.ingestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 변환 실패 이벤트 임시 저장소.
 * 현재는 인메모리로 구현. 추후 RDB/Kafka 등으로 교체 가능.
 */
@Component
class FailedEventStore {

    private val log = LoggerFactory.getLogger(FailedEventStore::class.java)
    private val store = ConcurrentLinkedQueue<FailedEvent>()

    fun save(event: FailedEvent) {
        store.add(event)
        log.warn("[FailedEventStore] 저장됨: eventId={}, platform={}, error={}",
            event.eventId, event.platform, event.errorMessage)
    }

    fun findAll(): List<FailedEvent> = store.toList()

    fun count(): Int = store.size
}
