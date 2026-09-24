package co.com.juandavidg.franchise_management.domain.usecase.product;

import co.com.juandavidg.franchise_management.domain.command.UpdateProductStockCommand;
import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateProductStockUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public class UpdateProductStockUseCaseImpl implements UpdateProductStockUseCase {

    private final ProductRepositoryPort productRepository;

    public UpdateProductStockUseCaseImpl(final ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<Product> execute(final UpdateProductStockCommand command) {
        return Mono.just(command)
                .filter(item -> item.delta() != null
                        && item.delta() >= UpdateProductStockCommand.MIN_DELTA
                        && item.delta() <= UpdateProductStockCommand.MAX_DELTA)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.STOCK_DELTA_OUT_OF_RANGE)))
                .flatMap(valid -> productRepository.updateStock(
                        valid.franchiseId(), valid.branchId(), valid.productId(), valid.delta())
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))));
    }
}
