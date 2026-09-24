package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private AddProductUseCaseImpl useCase;

    @Test
    void shouldAddProductWhenFranchiseAndBranchExistAndNameIsAvailable() {
        // ARRANGE
        final Product incoming = incomingProduct(10);
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.just(branch()));
        when(productRepository.existsByFranchiseIdAndBranchIdAndName("franchise-1", "branch-1", "Fries"))
                .thenReturn(Mono.just(false));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectNextMatches(product ->
                        product.getId() != null
                                && "franchise-1".equals(product.getFranchiseId())
                                && "branch-1".equals(product.getBranchId())
                                && "Fries".equals(product.getName())
                                && Integer.valueOf(10).equals(product.getStock()))
                .verifyComplete();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldRejectWhenStockIsNegative() {
        // ARRANGE
        final Product incoming = incomingProduct(-1);

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.INVALID_STOCK == businessException.getCode())
                .verify();
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        final Product incoming = incomingProduct(10);
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectWhenBranchDoesNotExist() {
        // ARRANGE
        final Product incoming = incomingProduct(10);
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.BRANCH_NOT_FOUND == businessException.getCode())
                .verify();
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectWhenProductNameAlreadyExists() {
        // ARRANGE
        final Product incoming = incomingProduct(10);
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.just(branch()));
        when(productRepository.existsByFranchiseIdAndBranchIdAndName("franchise-1", "branch-1", "Fries"))
                .thenReturn(Mono.just(true));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.PRODUCT_ALREADY_EXISTS == businessException.getCode())
                .verify();
        verify(productRepository, never()).save(any(Product.class));
    }

    private static Product incomingProduct(final Integer stock) {
        return Product.builder()
                .franchiseId("franchise-1")
                .branchId("branch-1")
                .name("Fries")
                .stock(stock)
                .build();
    }

    private static Franchise franchise() {
        return Franchise.builder()
                .id("franchise-1")
                .name("McDonald's")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Branch branch() {
        return Branch.builder()
                .id("branch-1")
                .franchiseId("franchise-1")
                .name("Downtown")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
