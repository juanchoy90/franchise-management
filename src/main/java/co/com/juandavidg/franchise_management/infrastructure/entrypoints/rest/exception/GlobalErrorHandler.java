package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception;

import co.com.juandavidg.franchise_management.domain.model.exceptions.DomainException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    private final HttpStatusResolver statusResolver;

    public GlobalErrorHandler(
            final ErrorAttributes errorAttributes,
            final WebProperties webProperties,
            final ApplicationContext applicationContext,
            final ServerCodecConfigurer codecConfigurer,
            final HttpStatusResolver statusResolver) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        setMessageWriters(codecConfigurer.getWriters());
        setMessageReaders(codecConfigurer.getReaders());
        this.statusResolver = statusResolver;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::render);
    }

    private Mono<ServerResponse> render(final ServerRequest request) {
        final Throwable error = getError(request);
        final String path = request.path();

        return Mono.just(error)
                .ofType(DomainException.class)
                .map(domainException -> new ResolvedError(domainException.getCode(), domainException.getCode().getMessage()))
                .switchIfEmpty(constraintError(error))
                .switchIfEmpty(inputError(error))
                .switchIfEmpty(Mono.fromSupplier(() -> unexpectedError(error, path)))
                .flatMap(resolved -> respond(resolved, request));
    }

    private Mono<ResolvedError> constraintError(final Throwable error) {
        return Mono.just(error)
                .ofType(ConstraintViolationException.class)
                .flatMap(exception -> Flux.fromIterable(exception.getConstraintViolations())
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .reduce((left, right) -> left + "; " + right)
                        .defaultIfEmpty(ErrorCode.VALIDATION_ERROR.getMessage())
                        .map(message -> new ResolvedError(ErrorCode.VALIDATION_ERROR, message)));
    }

    private Mono<ResolvedError> inputError(final Throwable error) {
        return Mono.just(error)
                .ofType(ServerWebInputException.class)
                .map(ignored -> new ResolvedError(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getMessage()));
    }

    private Mono<ServerResponse> respond(final ResolvedError resolved, final ServerRequest request) {
        final HttpStatus status = statusResolver.resolve(resolved.code());

        return resolveTraceId(request)
                .flatMap(traceId -> ServerResponse.status(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ErrorResponse.builder()
                                .code(resolved.code().name())
                                .message(resolved.message())
                                .path(request.path())
                                .timestamp(Instant.now())
                                .traceId(traceId)
                                .build()));
    }

    private Mono<String> resolveTraceId(final ServerRequest request) {
        return Mono.justOrEmpty(request.exchange().<String>getAttribute(TraceIdWebFilter.TRACE_ID))
                .switchIfEmpty(Mono.justOrEmpty(
                        request.exchange().getResponse().getHeaders().getFirst(TraceIdWebFilter.HEADER)))
                .defaultIfEmpty("n/a");
    }

    private ResolvedError unexpectedError(final Throwable error, final String path) {
        log.error("Unhandled error at {}", path, error);
        return new ResolvedError(ErrorCode.UNEXPECTED_ERROR, ErrorCode.UNEXPECTED_ERROR.getMessage());
    }

    private record ResolvedError(ErrorCode code, String message) {
    }
}
