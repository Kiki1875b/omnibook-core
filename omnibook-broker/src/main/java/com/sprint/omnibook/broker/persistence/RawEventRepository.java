package com.sprint.omnibook.broker.persistence;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 원본 이벤트 저장소.
 * Insert 전용으로 사용하며, Update/Delete 연산은 사용하지 않는다.
 */
public interface RawEventRepository extends MongoRepository<RawEventDocument, ObjectId> {
}
