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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBranchUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @InjectMocks
    private UpdateBranchUseCaseImpl useCase;

    @Test
    void shouldUpdateBranchNameWhenAvailable() {
        // ARRANGE
        final Branch incoming = incoming("Airport");
        final Branch updated = Branch.builder()
                .id("branch-1")
                .franchiseId("franchise-1")
                .name("Airport")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.updateName("franchise-1", "branch-1", "Airport")).thenReturn(Mono.just(updated));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectNextMatches(result ->
                        "branch-1".equals(result.getId()) && "Airport".equals(result.getName()))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Airport").toBuilder().franchiseId("missing").build()))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
        verify(branchRepository, never()).updateName("missing", "branch-1", "Airport");
    }

    @Test
    void shouldRejectWhenBranchDoesNotExist() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.updateName("franchise-1", "missing", "Airport")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Airport").toBuilder().id("missing").build()))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.BRANCH_NOT_FOUND == businessException.getCode())
                .verify();
    }

    @Test
    void shouldRejectWhenNewNameAlreadyExists() {
        // ARRANGE
        when(franchiseRepository.findById("franchise-1")).thenReturn(Mono.just(franchise()));
        when(branchRepository.updateName("franchise-1", "branch-1", "Airport"))
                .thenReturn(Mono.error(new BusinessException(ErrorCode.BRANCH_ALREADY_EXISTS)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming("Airport")))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.BRANCH_ALREADY_EXISTS == businessException.getCode())
                .verify();
    }

    private static Franchise franchise() {
        return Franchise.builder().id("franchise-1").name("McDonald's").build();
    }

    private static Branch incoming(final String name) {
        return Branch.builder()
                .id("branch-1")
                .franchiseId("franchise-1")
                .name(name)
                .build();
    }
}
