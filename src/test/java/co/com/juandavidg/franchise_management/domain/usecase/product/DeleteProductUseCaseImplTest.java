package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.command.DeleteProductCommand;
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
class DeleteProductUseCaseImplTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private DeleteProductUseCaseImpl useCase;

    @Test
    void shouldDeleteWhenProductExists() {
        // ARRANGE
        when(productRepository.findById("franchise-1", "branch-1", "product-1"))
                .thenReturn(Mono.just(product()));
        when(productRepository.delete("franchise-1", "branch-1", "product-1"))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(new DeleteProductCommand("franchise-1", "branch-1", "product-1")))
                .verifyComplete();
        verify(productRepository).delete("franchise-1", "branch-1", "product-1");
    }

    @Test
    void shouldRejectWhenProductDoesNotExist() {
        // ARRANGE
        when(productRepository.findById("franchise-1", "branch-1", "missing"))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(new DeleteProductCommand("franchise-1", "branch-1", "missing")))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.PRODUCT_NOT_FOUND == businessException.getCode())
                .verify();
        verify(productRepository, never()).delete("franchise-1", "branch-1", "missing");
    }

    private static Product product() {
        return Product.builder()
                .id("product-1")
                .franchiseId("franchise-1")
                .branchId("branch-1")
                .name("Fries")
                .stock(10)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
