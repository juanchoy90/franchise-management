package co.com.juandavidg.franchise_management.domain.usecase.branch;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateBranchUseCase;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

public class UpdateBranchUseCaseImpl implements UpdateBranchUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;

    public UpdateBranchUseCaseImpl(
            final FranchiseRepositoryPort franchiseRepository,
            final BranchRepositoryPort branchRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    public Mono<Branch> execute(final Branch branch) {
        return franchiseRepository.findById(branch.getFranchiseId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.FRANCHISE_NOT_FOUND)))
                .then(Mono.defer(() -> branchRepository.updateName(
                        branch.getFranchiseId(), branch.getId(), branch.getName())))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.BRANCH_NOT_FOUND)));
    }
}
