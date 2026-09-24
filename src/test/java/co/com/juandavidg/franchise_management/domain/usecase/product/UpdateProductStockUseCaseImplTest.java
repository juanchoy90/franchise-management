package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.command.UpdateProductStockCommand;
import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductStockUseCaseImplTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private UpdateProductStockUseCaseImpl useCase;

    @Test
    void shouldApplyDeltaWhenWithinLimits() {
        // ARRANGE
        final UpdateProductStockCommand command = command(15);
        when(productRepository.updateStock("franchise-1", "branch-1", "product-1", 15))
                .thenReturn(Mono.just(product(25)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectNextMatches(product ->
                        "product-1".equals(product.getId())
                                && Integer.valueOf(25).equals(product.getStock()))
                .verifyComplete();
        verify(productRepository).updateStock("franchise-1", "branch-1", "product-1", 15);
    }

    @Test
    void shouldAllowMinimumDelta() {
        // ARRANGE
        final UpdateProductStockCommand command = command(UpdateProductStockCommand.MIN_DELTA);
        when(productRepository.updateStock(
                "franchise-1", "branch-1", "product-1", UpdateProductStockCommand.MIN_DELTA))
                .thenReturn(Mono.just(product(0)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectNextMatches(product -> Integer.valueOf(0).equals(product.getStock()))
                .verifyComplete();
    }

    @Test
    void shouldAllowMaximumDelta() {
        // ARRANGE
        final UpdateProductStockCommand command = command(UpdateProductStockCommand.MAX_DELTA);
        when(productRepository.updateStock(
                "franchise-1", "branch-1", "product-1", UpdateProductStockCommand.MAX_DELTA))
                .thenReturn(Mono.just(product(10_000_010)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectNextMatches(product -> Integer.valueOf(10_000_010).equals(product.getStock()))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenDeltaExceedsMaximum() {
        // ARRANGE
        final UpdateProductStockCommand command = command(UpdateProductStockCommand.MAX_DELTA + 1);

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.STOCK_DELTA_OUT_OF_RANGE == businessException.getCode())
                .verify();
        verify(productRepository, never()).updateStock(
                "franchise-1", "branch-1", "product-1", UpdateProductStockCommand.MAX_DELTA + 1);
    }

    @Test
    void shouldRejectWhenDeltaIsBelowMinimum() {
        // ARRANGE
        final UpdateProductStockCommand command = command(UpdateProductStockCommand.MIN_DELTA - 1);

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.STOCK_DELTA_OUT_OF_RANGE == businessException.getCode())
                .verify();
        verify(productRepository, never()).updateStock(
                "franchise-1", "branch-1", "product-1", UpdateProductStockCommand.MIN_DELTA - 1);
    }

    @Test
    void shouldRejectWhenProductDoesNotExist() {
        // ARRANGE
        final UpdateProductStockCommand command = command(15);
        when(productRepository.updateStock("franchise-1", "branch-1", "product-1", 15))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.PRODUCT_NOT_FOUND == businessException.getCode())
                .verify();
    }

    private static UpdateProductStockCommand command(final Integer delta) {
        return new UpdateProductStockCommand("franchise-1", "branch-1", "product-1", delta);
    }

    private static Product product(final Integer stock) {
        return Product.builder()
                .id("product-1")
                .franchiseId("franchise-1")
                .branchId("branch-1")
                .name("Fries")
                .stock(stock)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
    }
}
