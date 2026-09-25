package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper;

import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.model.exceptions.TechnicalException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public final class DynamoExceptionTranslator {

    private DynamoExceptionTranslator() {
    }

    public static <T> Mono<T> translate(final Throwable error) {
        return translate(error, "");
    }

    public static <T> Mono<T> translate(final Throwable error, final String operationName) {
        return Mono.<T>error(error)
                .onErrorMap(CompletionException.class, DynamoExceptionTranslator::causeOrSelf)
                .onErrorMap(ExecutionException.class, DynamoExceptionTranslator::causeOrSelf)
                .onErrorMap(ConditionalCheckFailedException.class,
                        failed -> new BusinessException(conflictCode(failed, operationName), failed))
                .onErrorMap(TransactionCanceledException.class,
                        e -> new BusinessException(transactionConflictCode(operationName), e))
                .onErrorMap(CallNotPermittedException.class,
                        e -> new TechnicalException(ErrorCode.SERVICE_UNAVAILABLE, e))
                .onErrorMap(TimeoutException.class,
                        e -> new TechnicalException(ErrorCode.TIMEOUT, e))
                .onErrorMap(ProvisionedThroughputExceededException.class,
                        e -> new TechnicalException(ErrorCode.SERVICE_THROTTLED, e))
                .onErrorMap(RequestLimitExceededException.class,
                        e -> new TechnicalException(ErrorCode.SERVICE_THROTTLED, e))
                .onErrorMap(InternalServerErrorException.class,
                        e -> new TechnicalException(ErrorCode.PERSISTENCE_ERROR, e))
                .onErrorMap(ResourceNotFoundException.class,
                        e -> new TechnicalException(ErrorCode.PERSISTENCE_ERROR, e))
                .onErrorMap(DynamoDbException.class,
                        e -> new TechnicalException(ErrorCode.PERSISTENCE_ERROR, e))
                .onErrorMap(SdkException.class,
                        e -> new TechnicalException(ErrorCode.PERSISTENCE_ERROR, e));
    }

    private static ErrorCode conflictCode(
            final ConditionalCheckFailedException failed,
            final String operationName) {
        return Map.of(
                        "updateProductStock", stockUpdateCode(failed),
                        "updateBranchName", ErrorCode.BRANCH_ALREADY_EXISTS)
                .getOrDefault(operationName, ErrorCode.FRANCHISE_ALREADY_EXISTS);
    }

    private static ErrorCode transactionConflictCode(final String operationName) {
        return Map.of("updateBranchName", ErrorCode.BRANCH_ALREADY_EXISTS)
                .getOrDefault(operationName, ErrorCode.FRANCHISE_ALREADY_EXISTS);
    }

    private static ErrorCode stockUpdateCode(final ConditionalCheckFailedException failed) {
        return Map.of(true, ErrorCode.INSUFFICIENT_STOCK, false, ErrorCode.PRODUCT_NOT_FOUND)
                .get(failed.hasItem() && !failed.item().isEmpty());
    }

    private static Throwable causeOrSelf(final Throwable error) {
        return Objects.requireNonNullElse(error.getCause(), error);
    }
}
