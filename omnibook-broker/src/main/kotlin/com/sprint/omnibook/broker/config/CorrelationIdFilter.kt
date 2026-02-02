package com.sprint.omnibook.broker.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 모든 요청에 correlationId를 MDC에 설정하는 필터.
 * X-Correlation-Id 헤더가 있으면 사용, 없으면 생성한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val MDC_CORRELATION_ID = "correlationId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var correlationId = request.getHeader(CORRELATION_ID_HEADER)

        if (correlationId.isNullOrBlank()) {
            correlationId = generateCorrelationId()
        }

        try {
            MDC.put(MDC_CORRELATION_ID, correlationId)
            response.setHeader(CORRELATION_ID_HEADER, correlationId)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_CORRELATION_ID)
        }
    }

    private fun generateCorrelationId(): String = UUID.randomUUID().toString().substring(0, 8)
}
