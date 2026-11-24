package com.pivot.banking.transaction.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Propagates correlation identifiers through Reactor context, MDC and HTTP headers. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    /** Reuses the incoming identifier or generates one when it is absent or blank. */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().set("X-Correlation-ID", finalCorrelationId);
        MDC.put("correlationId", finalCorrelationId);
        return chain.filter(exchange)
                .contextWrite(context -> context.put("correlationId", finalCorrelationId))
                .doFinally(signal -> MDC.remove("correlationId"));
    }
}
