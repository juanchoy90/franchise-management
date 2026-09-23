package co.com.juandavidg.franchise_management.domain.ports.out;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepositoryPort {
    
    Mono<Franchise> save(final Franchise franchise);
    
    Mono<Franchise> findById(final String id);
    
    Mono<Franchise> updateName(final String id, final String newName);
    
    Mono<Boolean> existsByName(final String name);
}
