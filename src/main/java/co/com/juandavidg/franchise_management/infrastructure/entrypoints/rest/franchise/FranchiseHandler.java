package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise;

import co.com.juandavidg.franchise_management.domain.ports.in.CreateFranchiseUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.GetFranchiseUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.GetTopStockProductsUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateFranchiseUseCase;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.CreateFranchiseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.FranchiseResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.UpdateFranchiseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.FranchiseApi;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.BranchTopProductResponseDTO;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class FranchiseHandler implements FranchiseApi {

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final GetFranchiseUseCase getFranchiseUseCase;
    private final UpdateFranchiseUseCase updateFranchiseUseCase;
    private final GetTopStockProductsUseCase getTopStockProductsUseCase;
    private final Validator validator;

    public FranchiseHandler(
            final CreateFranchiseUseCase createFranchiseUseCase,
            final GetFranchiseUseCase getFranchiseUseCase,
            final UpdateFranchiseUseCase updateFranchiseUseCase,
            final GetTopStockProductsUseCase getTopStockProductsUseCase,
            final Validator validator) {
        this.createFranchiseUseCase = createFranchiseUseCase;
        this.getFranchiseUseCase = getFranchiseUseCase;
        this.updateFranchiseUseCase = updateFranchiseUseCase;
        this.getTopStockProductsUseCase = getTopStockProductsUseCase;
        this.validator = validator;
    }

    public Mono<ServerResponse> create(final ServerRequest request) {
        return request.bodyToMono(CreateFranchiseDTO.class)
                .flatMap(this::validate)
                .map(CreateFranchiseDTO::toDomain)
                .flatMap(createFranchiseUseCase::execute)
                .map(FranchiseResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.status(HttpStatus.CREATED).bodyValue(body));
    }

    public Mono<ServerResponse> findById(final ServerRequest request) {
        return Mono.just(request.pathVariable("id"))
                .flatMap(getFranchiseUseCase::execute)
                .map(FranchiseResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    public Mono<ServerResponse> update(final ServerRequest request) {
        return request.bodyToMono(UpdateFranchiseDTO.class)
                .flatMap(this::validate)
                .map(dto -> dto.toDomain(request.pathVariable("id")))
                .flatMap(updateFranchiseUseCase::execute)
                .map(FranchiseResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    public Mono<ServerResponse> findTopStock(final ServerRequest request) {
        return getTopStockProductsUseCase.execute(request.pathVariable("id"))
                .map(BranchTopProductResponseDTO::fromDomain)
                .collectList()
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    private <T> Mono<T> validate(final T dto) {
        return Mono.fromCallable(() -> validator.validate(dto))
                .flatMap(violations -> Mono.just(dto)
                        .filter(ignored -> violations.isEmpty())
                        .switchIfEmpty(Mono.error(new ConstraintViolationException(violations))));
    }
}
