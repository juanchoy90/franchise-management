package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product;

import co.com.juandavidg.franchise_management.domain.command.DeleteProductCommand;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.in.AddProductUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.DeleteProductUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateProductStockUseCase;
import co.com.juandavidg.franchise_management.domain.ports.in.UpdateProductUseCase;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.ProductApi;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.AddProductDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.ProductResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.UpdateProductDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.UpdateProductStockDTO;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ProductHandler implements ProductApi {

    private final AddProductUseCase addProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final Validator validator;

    public ProductHandler(
            final AddProductUseCase addProductUseCase,
            final DeleteProductUseCase deleteProductUseCase,
            final UpdateProductStockUseCase updateProductStockUseCase,
            final UpdateProductUseCase updateProductUseCase,
            final Validator validator) {
        this.addProductUseCase = addProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.validator = validator;
    }

    public Mono<ServerResponse> add(final ServerRequest request) {
        return request.bodyToMono(AddProductDTO.class)
                .flatMap(this::validate)
                .map(AddProductDTO::toDomain)
                .flatMap(addProductUseCase::execute)
                .map(ProductResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.status(HttpStatus.CREATED).bodyValue(body));
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return Mono.zip(
                        Mono.just(request.pathVariable("id")),
                        Mono.justOrEmpty(request.queryParam("franchiseId")).filter(id -> !id.isBlank()),
                        Mono.justOrEmpty(request.queryParam("branchId")).filter(id -> !id.isBlank()))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR)))
                .map(ids -> new DeleteProductCommand(ids.getT2(), ids.getT3(), ids.getT1()))
                .flatMap(deleteProductUseCase::execute)
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateStock(final ServerRequest request) {
        return Mono.zip(
                        Mono.just(request.pathVariable("id")),
                        Mono.justOrEmpty(request.queryParam("franchiseId")).filter(id -> !id.isBlank()),
                        Mono.justOrEmpty(request.queryParam("branchId")).filter(id -> !id.isBlank()),
                        request.bodyToMono(UpdateProductStockDTO.class).flatMap(this::validate))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR)))
                .map(parts -> parts.getT4().toCommand(parts.getT2(), parts.getT3(), parts.getT1()))
                .flatMap(updateProductStockUseCase::execute)
                .map(ProductResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    public Mono<ServerResponse> updateName(final ServerRequest request) {
        return Mono.zip(
                        Mono.just(request.pathVariable("id")),
                        Mono.justOrEmpty(request.queryParam("franchiseId")).filter(id -> !id.isBlank()),
                        Mono.justOrEmpty(request.queryParam("branchId")).filter(id -> !id.isBlank()),
                        request.bodyToMono(UpdateProductDTO.class).flatMap(this::validate))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR)))
                .map(parts -> parts.getT4().toDomain(parts.getT2(), parts.getT3(), parts.getT1()))
                .flatMap(updateProductUseCase::execute)
                .map(ProductResponseDTO::fromDomain)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    private <T> Mono<T> validate(final T dto) {
        return Mono.fromCallable(() -> validator.validate(dto))
                .flatMap(violations -> Mono.just(dto)
                        .filter(ignored -> violations.isEmpty())
                        .switchIfEmpty(Mono.error(new ConstraintViolationException(violations))));
    }
}
