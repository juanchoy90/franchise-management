package co.com.juandavidg.franchise_management.domain.usecase.branch;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.AddBranchUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public class AddBranchUseCaseImpl implements AddBranchUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;

    public AddBranchUseCaseImpl(
            final FranchiseRepositoryPort franchiseRepository,
            final BranchRepositoryPort branchRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    public Mono<Branch> execute(final Branch branch) {
        return franchiseRepository.findById(branch.getFranchiseId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)))
                .then(Mono.defer(() -> branchRepository.existsByFranchiseIdAndName(
                        branch.getFranchiseId(), branch.getName())))
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.BRANCH_ALREADY_EXISTS)))
                .then(Mono.defer(() -> persist(branch)));
    }

    private Mono<Branch> persist(final Branch branch) {
        final Instant now = Instant.now();
        return branchRepository.save(branch.toBuilder()
                .id(UUID.randomUUID().toString())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
