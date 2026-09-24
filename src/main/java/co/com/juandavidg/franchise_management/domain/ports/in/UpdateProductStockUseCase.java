package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.command.UpdateProductStockCommand;
import co.com.juandavidg.franchise_management.domain.model.Product;
import reactor.core.publisher.Mono;

public interface UpdateProductStockUseCase {

    Mono<Product> execute(final UpdateProductStockCommand command);
}
