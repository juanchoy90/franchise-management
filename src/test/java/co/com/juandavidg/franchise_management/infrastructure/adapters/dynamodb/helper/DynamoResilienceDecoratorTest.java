package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper;

import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.model.exceptions.TechnicalException;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

class DynamoResilienceDecoratorTest {

    private static final Duration TIMEOUT = Duration.ofMillis(80);
    private static final String INSTANCE = "dynamodb";

    private DynamoResilienceDecorator decorator;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(20)
                .failureRateThreshold(100f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        final RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(
                        ProvisionedThroughputExceededException.class,
                        SdkClientException.class)
                .build());
        final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(TIMEOUT)
                .cancelRunningFuture(true)
                .build());
        decorator = new DynamoResilienceDecorator(
                new DynamoDbProperties(
                        "FranchiseManagement",
                        "http://localhost:4566",
                        "us-east-1",
                        10,
                        3000L,
                        3000L,
                        3000L,
                        INSTANCE),
                circuitBreakerRegistry,
                retryRegistry,
                timeLimiterRegistry);
    }

    @Test
    void shouldReturnResultWhenOperationSucceeds() {
        // ARRANGE
        final Mono<String> operation = Mono.just("franchise");

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "saveFranchise"))
                .expectNext("franchise")
                .verifyComplete();
    }

    @Test
    void shouldRetryTransientThrottleAndSucceed() {
        // ARRANGE
        final AtomicInteger attempts = new AtomicInteger();
        final Mono<String> operation = Mono.defer(() -> attempts.incrementAndGet() < 3
                ? Mono.error(ProvisionedThroughputExceededException.builder().message("throttled").build())
                : Mono.just("franchise"));

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "saveFranchise"))
                .expectNext("franchise")
                .verifyComplete();
        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void shouldMapExhaustedRetryToThrottledError() {
        // ARRANGE
        final AtomicInteger attempts = new AtomicInteger();
        final Mono<String> operation = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(ProvisionedThroughputExceededException.builder().message("throttled").build());
        });

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "saveFranchise"))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.SERVICE_THROTTLED))
                .verify();
        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void shouldNotRetryBusinessConflict() {
        // ARRANGE
        final AtomicInteger attempts = new AtomicInteger();
        final Mono<String> operation = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(ConditionalCheckFailedException.builder().message("exists").build());
        });

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "saveFranchise"))
                .expectErrorMatches(error -> error instanceof BusinessException businessException
                        && ErrorCode.FRANCHISE_ALREADY_EXISTS == businessException.getCode())
                .verify();
        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    void shouldMapTimeoutToTechnicalTimeout() {
        // ARRANGE
        final Mono<String> operation = Mono.never();

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "findFranchiseById"))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.TIMEOUT))
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void shouldRejectCallsWhenCircuitIsOpen() {
        // ARRANGE
        circuitBreakerRegistry.circuitBreaker(INSTANCE).transitionToOpenState();
        final AtomicInteger attempts = new AtomicInteger();
        final Mono<String> operation = Mono.fromCallable(() -> {
            attempts.incrementAndGet();
            return "franchise";
        });

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "findFranchiseById"))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.SERVICE_UNAVAILABLE))
                .verify();
        Assertions.assertEquals(0, attempts.get());
    }

    @Test
    void shouldReturnFluxResultWhenOperationSucceeds() {
        // ARRANGE
        final Flux<String> operation = Flux.just("one", "two");

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "scanFranchises"))
                .expectNext("one", "two")
                .verifyComplete();
    }

    @Test
    void shouldRejectFluxWhenCircuitIsOpen() {
        // ARRANGE
        circuitBreakerRegistry.circuitBreaker(INSTANCE).transitionToOpenState();
        final Flux<String> operation = Flux.just("franchise");

        // ACT & ASSERT
        StepVerifier.create(decorator.decorate(operation, "scanFranchises"))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.SERVICE_UNAVAILABLE))
                .verify();
    }

    private static boolean isTechnical(final Throwable error, final ErrorCode code) {
        return error instanceof TechnicalException technicalException
                && code == technicalException.getCode();
    }
}
