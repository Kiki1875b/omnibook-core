package com.sprint.omnibook.broker.persistence

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * 원본 이벤트 저장용 MongoDB Document.
 * Append-only SoT로 동작하며, 업데이트 연산을 허용하지 않는다.
 * MongoDB ObjectId가 유일 식별자 역할을 한다.
 */
@Document(collection = "raw_events")
data class RawEventDocument(
    @Id
    val id: ObjectId? = null,

    val platform: String,

    val eventType: String,

    val correlationId: String?,

    /**
     * HTTP body 원본 그대로 저장.
     * 역직렬화 없이 수신된 JSON 문자열을 보존한다.
     */
    val rawBody: String,

    val receivedAt: Instant
)
