package co.com.juandavidg.franchise_management.domain.usecase.franchise;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateFranchiseUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

public class UpdateFranchiseUseCaseImpl implements UpdateFranchiseUseCase {

    private final FranchiseRepositoryPort repository;

    public UpdateFranchiseUseCaseImpl(final FranchiseRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> execute(final Franchise franchise) {
        return repository.updateName(franchise.getId(), franchise.getName())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)));
    }
}
