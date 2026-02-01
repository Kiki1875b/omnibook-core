package com.sprint.omnibook.broker.logging;

import com.sprint.omnibook.broker.ingestion.EventHeaders;
import com.sprint.omnibook.broker.ingestion.IngestionResult;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 이벤트 처리 로깅 Aspect.
 * MDC를 활용하여 eventId, correlationId, platform을 로깅 컨텍스트에 추가한다.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String MDC_EVENT_ID = "eventId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_PLATFORM = "platform";
    private static final String MDC_EVENT_TYPE = "eventType";

    @Around("execution(* com.sprint.omnibook.broker.ingestion.EventIngestionService.process(..))")
    public Object logIngestion(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        EventHeaders headers = extractHeaders(args);

        try {
            setupMdc(headers);
            log.info("이벤트 수신 시작");

            Object result = joinPoint.proceed();

            if (result instanceof IngestionResult ingestionResult) {
                MDC.put(MDC_EVENT_ID, ingestionResult.eventId());

                if (ingestionResult.success()) {
                    log.info("이벤트 처리 성공");
                } else {
                    log.warn("이벤트 처리 실패 - 재시도 대상으로 저장됨");
                }
            }

            return result;
        } catch (Exception e) {
            log.error("이벤트 처리 중 예외 발생: {}", e.getMessage());
            throw e;
        } finally {
            clearMdc();
        }
    }

    private EventHeaders extractHeaders(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof EventHeaders headers) {
                return headers;
            }
        }
        return null;
    }

    private void setupMdc(EventHeaders headers) {
        if (headers == null) return;

        if (headers.eventId() != null) {
            MDC.put(MDC_EVENT_ID, headers.eventId());
        }
        if (headers.correlationId() != null) {
            MDC.put(MDC_CORRELATION_ID, headers.correlationId());
        }
        if (headers.platform() != null) {
            MDC.put(MDC_PLATFORM, headers.platform());
        }
        if (headers.eventType() != null) {
            MDC.put(MDC_EVENT_TYPE, headers.eventType());
        }
    }

    private void clearMdc() {
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_PLATFORM);
        MDC.remove(MDC_EVENT_TYPE);
    }
}
