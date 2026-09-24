package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFilter implements WebFilter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        final String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        exchange.getAttributes().put(TRACE_ID, traceId);
        exchange.getResponse().getHeaders().set(HEADER, traceId);

        return chain.filter(exchange)
                .contextWrite(context -> context.put(TRACE_ID, traceId));
    }
}
