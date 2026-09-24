package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception.ErrorResponse;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.AddProductDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.ProductResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.UpdateProductStockDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    @Operation(
            operationId = "updateProductStock",
            tags = {"Products"},
            summary = "Adjust the stock of a product",
            description = "Applies an atomic stock delta. Concurrent adjustments do not overwrite each other. The resulting stock cannot be negative.",
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Product unique identifier",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    @Parameter(
                            name = "franchiseId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "Franchise that owns the product",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "550e8400-e29b-41d4-a716-446655440000")),
                    @Parameter(
                            name = "branchId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "Branch that owns the product",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
            },
            requestBody = @RequestBody(
                    required = true,
                    description = "Signed stock delta between -10000000 and 10000000",
                    content = @Content(
                            mediaType = JSON,
                            schema = @Schema(implementation = UpdateProductStockDTO.class),
                            examples = @ExampleObject(
                                    name = "updateProductStock",
                                    summary = "Valid stock increase",
                                    value = OpenApiExamples.UPDATE_PRODUCT_STOCK))),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Stock updated",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ProductResponseDTO.class),
                                    examples = @ExampleObject(name = "updated", value = OpenApiExamples.PRODUCT_STOCK_UPDATED))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Delta is missing, out of range, or franchiseId/branchId is missing",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "validationError", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The product does not exist",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "notFound", value = OpenApiExamples.PRODUCT_NOT_FOUND))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "The delta would make the stock negative",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "insufficientStock", value = OpenApiExamples.INSUFFICIENT_STOCK))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected persistence or infrastructure failure",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "unexpectedError", value = OpenApiExamples.UNEXPECTED_ERROR)))
            })
    Mono<ServerResponse> updateStock(final ServerRequest request);

    @Operation(
            operationId = "deleteProduct",
            tags = {"Products"},
            summary = "Delete a product from a branch",
            description = "Removes the product and releases its name so the same name can be reused in that branch.",
            parameters = {
                    @Parameter(
                            name = "id",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Product unique identifier",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")),
                    @Parameter(
                            name = "franchiseId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "Franchise that owns the product",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "550e8400-e29b-41d4-a716-446655440000")),
                    @Parameter(
                            name = "branchId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "Branch that owns the product",
                            schema = @Schema(
                                    type = "string",
                                    format = "uuid",
                                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Product deleted"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "franchiseId or branchId is missing",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "validationError", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The product does not exist",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "notFound", value = OpenApiExamples.PRODUCT_NOT_FOUND))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected persistence or infrastructure failure",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "unexpectedError", value = OpenApiExamples.UNEXPECTED_ERROR)))
            })
    Mono<ServerResponse> delete(final ServerRequest request);
}
