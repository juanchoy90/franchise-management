package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch;

import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.AddBranchUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateBranchUseCase;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.AddBranchDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.BranchResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.UpdateBranchDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.BranchApi;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class BranchHandler implements BranchApi {

    private final AddBranchUseCase addBranchUseCase;
    private final UpdateBranchUseCase updateBranchUseCase;
    private final Validator validator;

    public BranchHandler(
            final AddBranchUseCase addBranchUseCase,
            final UpdateBranchUseCase updateBranchUseCase,
            final Validator validator) {
        this.addBranchUseCase = addBranchUseCase;
        this.updateBranchUseCase = updateBranchUseCase;
        this.validator = validator;
    }

    public Mono<ServerResponse> add(final ServerRequest request) {
        return request.bodyToMono(AddBranchDTO.class)
                .flatMap(this::validate)
                .map(AddBranchDTO::toDomain)
                .flatMap(addBranchUseCase::execute)
                .map(BranchResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.status(HttpStatus.CREATED).bodyValue(body));
    }

    public Mono<ServerResponse> update(final ServerRequest request) {
        return Mono.zip(
                        Mono.just(request.pathVariable("id")),
                        Mono.justOrEmpty(request.queryParam("franchiseId")).filter(id -> !id.isBlank()),
                        request.bodyToMono(UpdateBranchDTO.class).flatMap(this::validate))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR)))
                .map(parts -> parts.getT3().toDomain(parts.getT2(), parts.getT1()))
                .flatMap(updateBranchUseCase::execute)
                .map(BranchResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    private <T> Mono<T> validate(final T dto) {
        return Mono.fromCallable(() -> validator.validate(dto))
                .flatMap(violations -> Mono.just(dto)
                        .filter(ignored -> violations.isEmpty())
                        .switchIfEmpty(Mono.error(new ConstraintViolationException(violations))));
    }
}
