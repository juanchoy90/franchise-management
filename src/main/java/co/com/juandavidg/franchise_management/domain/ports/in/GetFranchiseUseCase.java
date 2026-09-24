package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import reactor.core.publisher.Mono;

public interface GetFranchiseUseCase {

    Mono<Franchise> execute(final String id);
}
