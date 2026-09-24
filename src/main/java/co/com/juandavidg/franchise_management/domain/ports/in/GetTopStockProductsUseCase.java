package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.model.BranchTopProduct;
import reactor.core.publisher.Flux;

public interface GetTopStockProductsUseCase {

    Flux<BranchTopProduct> execute(final String franchiseId);
}
