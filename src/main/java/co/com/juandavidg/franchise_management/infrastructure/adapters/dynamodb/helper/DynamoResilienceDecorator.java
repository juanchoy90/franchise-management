package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper;

import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DynamoResilienceDecorator {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public DynamoResilienceDecorator(
            final DynamoDbProperties properties,
            final CircuitBreakerRegistry circuitBreakerRegistry,
            final RetryRegistry retryRegistry,
            final TimeLimiterRegistry timeLimiterRegistry) {
        final String instance = properties.resilienceInstance();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(instance);
        this.retry = retryRegistry.retry(instance);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(instance);
    }

    public <T> Mono<T> decorate(final Mono<T> operation, final String operationName) {
        return operation
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnError(error -> log.error("DynamoDB operation [{}] failed", operationName, error))
                .onErrorResume(DynamoExceptionTranslator::translate);
    }

    public <T> Flux<T> decorate(final Flux<T> operation, final String operationName) {
        return operation
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnError(error -> log.error("DynamoDB operation [{}] failed", operationName, error))
                .onErrorResume(error -> DynamoExceptionTranslator.<T>translate(error));
    }
}
