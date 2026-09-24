package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import reactor.core.publisher.Mono;

public interface UpdateFranchiseUseCase {

    Mono<Franchise> execute(final Franchise franchise);
}
