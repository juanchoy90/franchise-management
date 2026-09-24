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
class GetFranchiseUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort repository;

    @InjectMocks
    private GetFranchiseUseCaseImpl useCase;

    @Test
    void shouldReturnFranchiseWhenItExists() {
        // ARRANGE
        final Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Subway")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById("franchise-1")).thenReturn(Mono.just(franchise));

        // ACT & ASSERT
        StepVerifier.create(useCase.execute("franchise-1"))
                .expectNextMatches(found ->
                        "franchise-1".equals(found.getId()) && "Subway".equals(found.getName()))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenFranchiseDoesNotExist() {
        // ARRANGE
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // ACT & ASSERT
        StepVerifier.create(useCase.execute("missing"))
                .expectErrorMatches(error ->
                        error instanceof BusinessException businessException
                                && ErrorCode.FRANCHISE_NOT_FOUND == businessException.getCode())
                .verify();
    }
}
