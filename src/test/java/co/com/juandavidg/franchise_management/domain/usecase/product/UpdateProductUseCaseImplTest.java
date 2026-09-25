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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private UpdateProductUseCaseImpl useCase;

    @Test
    void shouldUpdateProductNameWhenAvailable() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.just(branch()));
        when(productRepository.updateName("franchise-1", "branch-1", "product-1", "Burger"))
                .thenReturn(Mono.just(product("Burger")));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Burger")))
                .expectNextMatches(result ->
                        "product-1".equals(result.getId()) && "Burger".equals(result.getName()))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Burger").toBuilder().franchiseId("missing").build()))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
        verify(productRepository, never()).updateName("missing", "branch-1", "product-1", "Burger");
    }

    @Test
    void shouldRejectWhenBranchDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Burger").toBuilder().branchId("missing").build()))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.BRANCH_NOT_FOUND == businessException.getCode())
                .verify();
        verify(productRepository, never()).updateName("franchise-1", "missing", "product-1", "Burger");
    }

    @Test
    void shouldRejectWhenProductDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.just(branch()));
        when(productRepository.updateName("franchise-1", "branch-1", "missing", "Burger")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Burger").toBuilder().id("missing").build()))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.PRODUCT_NOT_FOUND == businessException.getCode())
                .verify();
    }

    @Test
    void shouldRejectWhenNewNameAlreadyExists() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.findById("franchise-1", "branch-1")).thenReturn(Mono.just(branch()));
        when(productRepository.updateName("franchise-1", "branch-1", "product-1", "Burger"))
                .thenReturn(Mono.error(new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Burger")))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.PRODUCT_ALREADY_EXISTS == businessException.getCode())
                .verify();
    }

    private static Franchise franchise() {
        return Franchise.builder().id("franchise-1").name("McDonald's").build();
    }

    private static Branch branch() {
        return Branch.builder().id("branch-1").franchiseId("franchise-1").name("Downtown").build();
    }

    private static Product incoming(final String name) {
        return Product.builder()
                .id("product-1")
                .franchiseId("franchise-1")
                .branchId("branch-1")
                .name(name)
                .build();
    }

    private static Product product(final String name) {
        return Product.builder()
                .id("product-1")
                .franchiseId("franchise-1")
                .branchId("branch-1")
                .name(name)
                .stock(10)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
    }
}
