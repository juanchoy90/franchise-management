package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import reactor.core.publisher.Mono;

public interface AddBranchUseCase {

    Mono<Branch> execute(final Branch branch);
}
