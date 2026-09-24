package co.com.juandavidg.franchise_management.domain.usecase.franchise;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.GetFranchiseUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

public class GetFranchiseUseCaseImpl implements GetFranchiseUseCase {

    private final FranchiseRepositoryPort repository;

    public GetFranchiseUseCaseImpl(final FranchiseRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> execute(final String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)));
    }
}
