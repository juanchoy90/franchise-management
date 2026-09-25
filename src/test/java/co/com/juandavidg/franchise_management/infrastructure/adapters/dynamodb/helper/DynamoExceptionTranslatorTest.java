package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper;

import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.DomainException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.model.exceptions.TechnicalException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

class DynamoExceptionTranslatorTest {

    @Test
    void shouldMapConditionalCheckToAlreadyExists() {
        // ARRANGE
        final Throwable error = ConditionalCheckFailedException.builder().message("exists").build();

        // ACT & ASSERT
        expectMapped(error, BusinessException.class, ErrorCode.FRANCHISE_ALREADY_EXISTS);
    }

    @Test
    void shouldMapBranchRenameCollisionToBranchAlreadyExists() {
        // ARRANGE
        final Throwable error = TransactionCanceledException.builder().message("canceled").build();

        // ACT & ASSERT
        StepVerifier.create(DynamoExceptionTranslator.<Object>translate(error, "updateBranchName"))
                .expectErrorMatches(mapped -> mapped instanceof BusinessException businessException
                        && ErrorCode.BRANCH_ALREADY_EXISTS == businessException.getCode())
                .verify();
    }

    @Test
    void shouldMapTransactionCanceledToAlreadyExists() {
        // ARRANGE
        final Throwable error = TransactionCanceledException.builder().message("canceled").build();

        // ACT & ASSERT
        expectMapped(error, BusinessException.class, ErrorCode.FRANCHISE_ALREADY_EXISTS);
    }

    @Test
    void shouldMapOpenCircuitToUnavailable() {
        // ARRANGE
        final CircuitBreaker circuitBreaker = CircuitBreaker.of("dynamodb", CircuitBreakerConfig.ofDefaults());
        circuitBreaker.transitionToOpenState();
        final Throwable error = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldMapTimeoutToTimeout() {
        // ARRANGE
        final Throwable error = new TimeoutException("slow");

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.TIMEOUT);
    }

    @Test
    void shouldMapThroughputExceededToThrottled() {
        // ARRANGE
        final Throwable error = ProvisionedThroughputExceededException.builder().message("throttled").build();

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.SERVICE_THROTTLED);
    }

    @Test
    void shouldMapRequestLimitToThrottled() {
        // ARRANGE
        final Throwable error = RequestLimitExceededException.builder().message("limited").build();

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.SERVICE_THROTTLED);
    }

    @Test
    void shouldMapInternalServerErrorToPersistenceError() {
        // ARRANGE
        final Throwable error = InternalServerErrorException.builder().message("internal").build();

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.PERSISTENCE_ERROR);
    }

    @Test
    void shouldMapResourceNotFoundToPersistenceError() {
        // ARRANGE
        final Throwable error = ResourceNotFoundException.builder().message("missing table").build();

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.PERSISTENCE_ERROR);
    }

    @Test
    void shouldMapGenericDynamoExceptionToPersistenceError() {
        // ARRANGE
        final Throwable error = DynamoDbException.builder().message("dynamo").build();

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.PERSISTENCE_ERROR);
    }

    @Test
    void shouldMapSdkExceptionToPersistenceError() {
        // ARRANGE
        final Throwable error = SdkClientException.create("network");

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.PERSISTENCE_ERROR);
    }

    @Test
    void shouldUnwrapCompletionExceptionBeforeMapping() {
        // ARRANGE
        final Throwable error = new CompletionException(
                ProvisionedThroughputExceededException.builder().message("throttled").build());

        // ACT & ASSERT
        expectMapped(error, TechnicalException.class, ErrorCode.SERVICE_THROTTLED);
    }

    private static void expectMapped(
            final Throwable error,
            final Class<? extends DomainException> type,
            final ErrorCode code) {
        StepVerifier.create(DynamoExceptionTranslator.<Object>translate(error))
                .expectErrorMatches(mapped -> type.isInstance(mapped)
                        && code == ((DomainException) mapped).getCode())
                .verify();
    }
}
