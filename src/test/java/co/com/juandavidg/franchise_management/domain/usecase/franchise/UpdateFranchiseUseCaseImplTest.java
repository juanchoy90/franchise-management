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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFranchiseUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort repository;

    @InjectMocks
    private UpdateFranchiseUseCaseImpl useCase;

    @Test
    void shouldUpdateFranchiseNameWhenAvailable() {
        // ARRANGE
        final Franchise incoming = Franchise.builder()
                .id("franchise-1")
                .name("Popeyes")
                .build();
        final Franchise updated = Franchise.builder()
                .id("franchise-1")
                .name("Popeyes")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
        when(repository.updateName("franchise-1", "Popeyes")).thenReturn(Mono.just(updated));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectNextMatches(result ->
                        "franchise-1".equals(result.getId()) && "Popeyes".equals(result.getName()))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        final Franchise incoming = Franchise.builder()
                .id("missing")
                .name("Popeyes")
                .build();
        when(repository.updateName("missing", "Popeyes")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
    }

    @Test
    void shouldRejectWhenNewNameAlreadyExists() {
        // ARRANGE
        final Franchise incoming = Franchise.builder()
                .id("franchise-1")
                .name("KFC")
                .build();
        when(repository.updateName("franchise-1", "KFC"))
                .thenReturn(Mono.error(new BusinessException(ErrorCode.FRANCHISE_ALREADY_EXISTS)));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute(incoming))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_ALREADY_EXISTS == businessException.getCode())
                .verify();
    }
}
