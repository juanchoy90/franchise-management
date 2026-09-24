package co.com.juandavidg.franchise_management.domain.ports.out;

import co.com.juandavidg.franchise_management.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {

    Mono<Product> save(final Product product);

    Mono<Boolean> existsByFranchiseIdAndBranchIdAndName(
            final String franchiseId,
            final String branchId,
            final String name);

    Mono<Product> findById(final String franchiseId, final String branchId, final String productId);

    Mono<Void> delete(final String franchiseId, final String branchId, final String productId);

    Mono<Product> updateStock(
            final String franchiseId,
            final String branchId,
            final String productId,
            final Integer delta);

    Mono<Product> findTopStock(final String franchiseId, final String branchId);
}
