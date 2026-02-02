package com.sprint.omnibook.broker.logging

import com.sprint.omnibook.broker.ingestion.EventHeaders
import com.sprint.omnibook.broker.ingestion.IngestionResult
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * 이벤트 처리 로깅 Aspect.
 * MDC를 활용하여 eventId, correlationId, platform을 로깅 컨텍스트에 추가한다.
 */
@Aspect
@Component
class LoggingAspect {

    private val log = LoggerFactory.getLogger(LoggingAspect::class.java)

    companion object {
        private const val MDC_EVENT_ID = "eventId"
        private const val MDC_CORRELATION_ID = "correlationId"
        private const val MDC_PLATFORM = "platform"
        private const val MDC_EVENT_TYPE = "eventType"
    }

    @Around("execution(* com.sprint.omnibook.broker.ingestion.EventIngestionService.process(..))")
    fun logIngestion(joinPoint: ProceedingJoinPoint): Any? {
        val args = joinPoint.args
        val headers = extractHeaders(args)

        return try {
            setupMdc(headers)
            log.info("이벤트 수신 시작")

            val result = joinPoint.proceed()

            if (result is IngestionResult) {
                MDC.put(MDC_EVENT_ID, result.eventId)

                if (result.success) {
                    log.info("이벤트 처리 성공")
                } else {
                    log.warn("이벤트 처리 실패: reason={}", result.failureReason)
                }
            }

            result
        } catch (e: Exception) {
            log.error("이벤트 처리 중 예외 발생: {}", e.message)
            throw e
        } finally {
            clearMdc()
        }
    }

    private fun extractHeaders(args: Array<Any?>): EventHeaders? {
        for (arg in args) {
            if (arg is EventHeaders) {
                return arg
            }
        }
        return null
    }

    private fun setupMdc(headers: EventHeaders?) {
        if (headers == null) return

        if (headers.eventId.isNotBlank()) {
            MDC.put(MDC_EVENT_ID, headers.eventId)
        }
        if (headers.correlationId.isNotBlank()) {
            MDC.put(MDC_CORRELATION_ID, headers.correlationId)
        }
        if (headers.platform.isNotBlank()) {
            MDC.put(MDC_PLATFORM, headers.platform)
        }
        if (headers.eventType.isNotBlank()) {
            MDC.put(MDC_EVENT_TYPE, headers.eventType)
        }
    }

    private fun clearMdc() {
        MDC.remove(MDC_EVENT_ID)
        MDC.remove(MDC_CORRELATION_ID)
        MDC.remove(MDC_PLATFORM)
        MDC.remove(MDC_EVENT_TYPE)
    }
}
