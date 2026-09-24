package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.BranchTopProduct;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.GetTopStockProductsUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GetTopStockProductsUseCaseImpl implements GetTopStockProductsUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public GetTopStockProductsUseCaseImpl(
            final FranchiseRepositoryPort franchiseRepository,
            final BranchRepositoryPort branchRepository,
            final ProductRepositoryPort productRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Flux<BranchTopProduct> execute(final String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)))
                .flatMapMany(franchise -> branchRepository.findByFranchiseId(franchise.getId()))
                .concatMap(branch -> topProductOf(franchiseId, branch));
    }

    private Mono<BranchTopProduct> topProductOf(final String franchiseId, final Branch branch) {
        return productRepository.findTopStock(franchiseId, branch.getId())
                .map(product -> new BranchTopProduct(branch.getId(), branch.getName(), product))
                .defaultIfEmpty(new BranchTopProduct(branch.getId(), branch.getName(), null));
    }
}
