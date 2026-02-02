package com.sprint.omnibook.broker.persistence;

import com.sprint.omnibook.broker.ingestion.EventHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 원본 이벤트 저장 서비스.
 * MongoDB Append-only SoT 역할을 수행한다.
 * 파싱 없이 원본을 즉시 저장하는 것이 유일한 책임이다.
 */
@Service
@RequiredArgsConstructor
public class RawEventService {

    private final RawEventRepository rawEventRepository;

    /**
     * raw body를 즉시 저장한다.
     * 파싱 없이 원본 그대로 저장하여 유실을 방지한다.
     *
     * @param rawBody HTTP body 원본
     * @param headers HTTP 헤더 정보
     */
    public void store(String rawBody, EventHeaders headers) {
        RawEventDocument document = RawEventDocument.builder()
                .platform(headers.platform())
                .eventType(headers.eventType())
                .correlationId(headers.correlationId())
                .rawBody(rawBody)
                .receivedAt(Instant.now())
                .build();

        rawEventRepository.save(document);
    }
}
