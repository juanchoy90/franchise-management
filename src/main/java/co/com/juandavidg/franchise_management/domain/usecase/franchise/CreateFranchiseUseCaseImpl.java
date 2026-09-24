package co.com.juandavidg.franchise_management.domain.usecase.franchise;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.CreateFranchiseUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public class CreateFranchiseUseCaseImpl implements CreateFranchiseUseCase {

    private final FranchiseRepositoryPort repository;

    public CreateFranchiseUseCaseImpl(final FranchiseRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> execute(final Franchise franchise) {
        return repository.existsByName(franchise.getName())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_ALREADY_EXISTS)))
                .then(Mono.defer(() -> persist(franchise)));
    }

    private Mono<Franchise> persist(final Franchise franchise) {
        final Instant now = Instant.now();
        return repository.save(franchise.toBuilder()
                .id(UUID.randomUUID().toString())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
