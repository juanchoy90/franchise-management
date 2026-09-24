package co.com.juandavidg.franchise_management.domain.usecase.franchise;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
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
class CreateFranchiseUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort repository;

    @InjectMocks
    private CreateFranchiseUseCaseImpl useCase;

    @Test
    void shouldCreateFranchiseWhenNameIsAvailable() {
        // ARRANGE
        final Franchise incoming = Franchise.builder().name("McDonald's").build();
        final Franchise persisted = Franchise.builder()
                .id("franchise-1")
                .name("McDonald's")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.existsByName("McDonald's")).thenReturn(Mono.just(false));
        when(repository.save(any(Franchise.class))).thenReturn(Mono.just(persisted));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectNextMatches(created ->
                        "franchise-1".equals(created.getId()) && "McDonald's".equals(created.getName()))
                .verifyComplete();
        verify(repository).save(any(Franchise.class));
    }

    @Test
    void shouldRejectDuplicateFranchiseName() {
        // ARRANGE
        final Franchise incoming = Franchise.builder().name("McDonald's").build();
        when(repository.existsByName("McDonald's")).thenReturn(Mono.just(true));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_ALREADY_EXISTS == businessException.getCode())
                .verify();
        verify(repository, never()).save(any(Franchise.class));
    }
}
