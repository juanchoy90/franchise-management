package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.model.Product;
import reactor.core.publisher.Mono;

public interface UpdateProductUseCase {

    Mono<Product> execute(final Product product);
}
