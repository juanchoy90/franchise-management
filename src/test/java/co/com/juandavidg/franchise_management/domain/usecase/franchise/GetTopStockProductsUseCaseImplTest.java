package co.com.juandavidg.franchise_management.domain.usecase.franchise;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductsUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private GetTopStockProductsUseCaseImpl useCase;

    @Test
    void shouldReturnTheHighestStockProductOfEachBranch() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1"))
                .thenReturn(Mono.just(Franchise.builder().id("franchise-1").name("McDonald's").build()));
        when(branchRepository.findByFranchiseId("franchise-1"))
                .thenReturn(Flux.just(branch("branch-1"), branch("branch-2")));
        when(productRepository.findTopStock("franchise-1", "branch-1"))
                .thenReturn(Mono.just(product("branch-1", "Fries", 10)));
        when(productRepository.findTopStock("franchise-1", "branch-2"))
                .thenReturn(Mono.just(product("branch-2", "Burger", 40)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute("franchise-1"))
                .expectNextMatches(item -> "branch-1".equals(item.branchId())
                        && Integer.valueOf(10).equals(item.product().getStock()))
                .expectNextMatches(item -> "branch-2".equals(item.branchId())
                        && Integer.valueOf(40).equals(item.product().getStock()))
                .verifyComplete();
    }

    @Test
    void shouldListBranchesWithoutProductsWithoutTopProduct() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1"))
                .thenReturn(Mono.just(Franchise.builder().id("franchise-1").name("McDonald's").build()));
        when(branchRepository.findByFranchiseId("franchise-1"))
                .thenReturn(Flux.just(branch("branch-1"), branch("branch-2")));
        when(productRepository.findTopStock("franchise-1", "branch-1"))
                .thenReturn(Mono.just(product("branch-1", "Fries", 10)));
        when(productRepository.findTopStock("franchise-1", "branch-2"))
                .thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute("franchise-1"))
                .expectNextMatches(item -> "branch-1".equals(item.branchId()) && item.product() != null)
                .expectNextMatches(item -> "branch-2".equals(item.branchId()) && item.product() == null)
                .verifyComplete();
    }

    @Test
    void shouldFailWhenFranchiseDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute("missing"))
                .expectErrorMatches(error -> error instanceof BusinessException businessException
                        && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
        verify(branchRepository, never()).findByFranchiseId("missing");
    }

    private static Branch branch(final String branchId) {
        return Branch.builder().id(branchId).franchiseId("franchise-1").name(branchId).build();
    }

    private static Product product(final String branchId, final String name, final Integer stock) {
        return Product.builder()
                .id(branchId + "-product")
                .franchiseId("franchise-1")
                .branchId(branchId)
                .name(name)
                .stock(stock)
                .build();
    }
}
