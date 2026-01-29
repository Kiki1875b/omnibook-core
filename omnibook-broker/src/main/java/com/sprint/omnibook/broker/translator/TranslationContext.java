package com.sprint.omnibook.broker.translator;

import com.sprint.omnibook.broker.event.EventType;

/**
 * Translator → Mapper로 전달되는 컨텍스트.
 * MapStruct @Context로 사용되어 eventType, rawPayload를 매핑에 포함시킨다.
 */
public record TranslationContext(
        EventType eventType,
        String rawPayload
) {}
