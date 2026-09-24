package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.AddProductUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public class AddProductUseCaseImpl implements AddProductUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public AddProductUseCaseImpl(
            final FranchiseRepositoryPort franchiseRepository,
            final BranchRepositoryPort branchRepository,
            final ProductRepositoryPort productRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Product> execute(final Product product) {
        return Mono.just(product)
                .filter(item -> item.getStock() != null && item.getStock() >= 0)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_STOCK)))
                .flatMap(valid -> franchiseRepository.findById(valid.getFranchiseId())
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)))
                        .then(Mono.defer(() -> branchRepository.findById(
                                valid.getFranchiseId(), valid.getBranchId())))
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.BRANCH_NOT_FOUND)))
                        .then(Mono.defer(() -> productRepository.existsByFranchiseIdAndBranchIdAndName(
                                valid.getFranchiseId(), valid.getBranchId(), valid.getName())))
                        .filter(exists -> !exists)
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS)))
                        .then(Mono.defer(() -> persist(valid))));
    }

    private Mono<Product> persist(final Product product) {
        final Instant now = Instant.now();
        return productRepository.save(product.toBuilder()
                .id(UUID.randomUUID().toString())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
