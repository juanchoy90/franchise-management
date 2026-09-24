package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception.ErrorResponse;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.AddProductDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.ProductResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface ProductApi {

    String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Operation(
            operationId = "addProduct",
            tags = {"Products"},
            summary = "Add a product to a branch",
            description = "Registers a new product under an existing branch. The product name must be unique within that branch and stock cannot be negative.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Product to add",
                    content = @Content(
                            mediaType = JSON,
                            schema = @Schema(implementation = AddProductDTO.class),
                            examples = @ExampleObject(
                                    name = "addProduct",
                                    summary = "Valid product",
                                    value = OpenApiExamples.CREATE_PRODUCT))),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Product created",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ProductResponseDTO.class),
                                    examples = @ExampleObject(name = "created", value = OpenApiExamples.PRODUCT))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request body failed syntactic validation or stock is invalid",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "validationError", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The franchise or branch does not exist",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "franchiseNotFound", value = OpenApiExamples.FRANCHISE_NOT_FOUND),
                                            @ExampleObject(name = "branchNotFound", value = OpenApiExamples.BRANCH_NOT_FOUND)
                                    })),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A product with the same name already exists in the branch",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "duplicateName", value = OpenApiExamples.DUPLICATE_PRODUCT))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected persistence or infrastructure failure",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "unexpectedError", value = OpenApiExamples.UNEXPECTED_ERROR)))
            })
    Mono<ServerResponse> add(final ServerRequest request);
}
