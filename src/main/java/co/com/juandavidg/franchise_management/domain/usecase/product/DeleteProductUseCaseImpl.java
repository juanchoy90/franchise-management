package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.command.DeleteProductCommand;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.DeleteProductUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public class DeleteProductUseCaseImpl implements DeleteProductUseCase {

    private final ProductRepositoryPort productRepository;

    public DeleteProductUseCaseImpl(final ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Void> execute(final DeleteProductCommand command) {
        return productRepository.findById(command.franchiseId(), command.branchId(), command.productId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .then(Mono.defer(() -> productRepository.delete(
                        command.franchiseId(), command.branchId(), command.productId())));
    }
}
