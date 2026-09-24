package co.com.juandavidg.franchise_management.domain.usecase.branch;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddBranchUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @InjectMocks
    private AddBranchUseCaseImpl useCase;

    @Test
    void shouldAddBranchWhenFranchiseExistsAndNameIsAvailable() {
        // ARRANGE
        final Branch incoming = incomingBranch("franchise-1", "Downtown");
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.existsByFranchiseIdAndName("franchise-1", "Downtown")).thenReturn(Mono.just(false));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectNextMatches(branch ->
                        branch.getId() != null
                                && "franchise-1".equals(branch.getFranchiseId())
                                && "Downtown".equals(branch.getName()))
                .verifyComplete();
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        final Branch incoming = incomingBranch("missing", "Downtown");
        when(franchiseRepository.findById("missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
        verify(branchRepository, never()).save(any(Branch.class));
    }

    @Test
    void shouldRejectWhenBranchNameAlreadyExists() {
        // ARRANGE
        final Branch incoming = incomingBranch("franchise-1", "Downtown");
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.existsByFranchiseIdAndName("franchise-1", "Downtown")).thenReturn(Mono.just(true));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.BRANCH_ALREADY_EXISTS == businessException.getCode())
                .verify();
        verify(branchRepository, never()).save(any(Branch.class));
    }

    private static Branch incomingBranch(final String franchiseId, final String name) {
        return Branch.builder()
                .franchiseId(franchiseId)
                .name(name)
                .build();
    }

    private static Franchise franchise() {
        return Franchise.builder()
                .id("franchise-1")
                .name("McDonald's")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
