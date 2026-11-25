package com.pivot.banking.transaction.infrastructure.config;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/** Propagates correlation identifiers through Reactor context, MDC and HTTP headers. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

  /** Reuses the incoming identifier or generates one when it is absent or blank. */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    String finalCorrelationId = correlationId;
    String method = String.valueOf(exchange.getRequest().getMethod());
    exchange.getResponse().getHeaders().set("X-Correlation-ID", finalCorrelationId);
    return chain
        .filter(exchange)
        .contextWrite(context -> context.put("correlationId", finalCorrelationId))
        .doOnSubscribe(
            ignored ->
                withCorrelationId(
                    finalCorrelationId,
                    () -> LOGGER.info("HTTP request started method={}", method)))
        .doFinally(
            signal -> {
              var statusCode = exchange.getResponse().getStatusCode();
              int status = statusCode == null ? 0 : statusCode.value();
              String statusLabel = statusCode == null ? "unknown" : Integer.toString(status);
              withCorrelationId(
                  finalCorrelationId,
                  () -> {
                    if (signal == SignalType.ON_ERROR || status >= 500) {
                      LOGGER.error(
                          "HTTP request failed method={} status={} signal={}",
                          method,
                          statusLabel,
                          signal);
                    } else if (status >= 400) {
                      LOGGER.warn(
                          "HTTP request completed method={} status={}", method, statusLabel);
                    } else {
                      LOGGER.info(
                          "HTTP request completed method={} status={}", method, statusLabel);
                    }
                  });
            });
  }

  private static void withCorrelationId(String correlationId, Runnable logAction) {
    String previousCorrelationId = MDC.get("correlationId");
    MDC.put("correlationId", correlationId);
    try {
      logAction.run();
    } finally {
      if (previousCorrelationId == null) {
        MDC.remove("correlationId");
      } else {
        MDC.put("correlationId", previousCorrelationId);
      }
    }
  }
}
