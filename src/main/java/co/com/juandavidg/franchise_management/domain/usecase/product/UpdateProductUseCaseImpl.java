package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateProductUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public class UpdateProductUseCaseImpl implements UpdateProductUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public UpdateProductUseCaseImpl(
            final FranchiseRepositoryPort franchiseRepository,
            final BranchRepositoryPort branchRepository,
            final ProductRepositoryPort productRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Product> execute(final Product product) {
        return franchiseRepository.findById(product.getFranchiseId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)))
                .then(Mono.defer(() -> branchRepository.findById(product.getFranchiseId(), product.getBranchId())))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.BRANCH_NOT_FOUND)))
                .then(Mono.defer(() -> productRepository.updateName(
                        product.getFranchiseId(), product.getBranchId(), product.getId(), product.getName())))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));
    }
}
