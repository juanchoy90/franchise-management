package co.com.juandavidg.franchise_management.domain.ports.out;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import reactor.core.publisher.Mono;

public interface BranchRepositoryPort {

    Mono<Branch> save(final Branch branch);

    Mono<Boolean> existsByFranchiseIdAndName(final String franchiseId, final String name);
}
