package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception.ErrorResponse;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.CreateFranchiseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.FranchiseResponseDTO;

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

public interface FranchiseApi {

    String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Operation(
            operationId = "createFranchise",
            tags = {"Franchises"},
            summary = "Create a franchise",
            description = "Registers a new franchise with a unique commercial name.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Franchise to create",
                    content = @Content(
                            mediaType = JSON,
                            schema = @Schema(implementation = CreateFranchiseDTO.class),
                            examples = @ExampleObject(
                                    name = "createFranchise",
                                    summary = "Valid franchise",
                                    value = OpenApiExamples.CREATE_FRANCHISE))),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Franchise created",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = FranchiseResponseDTO.class),
                                    examples = @ExampleObject(name = "created", value = OpenApiExamples.FRANCHISE))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request body failed syntactic validation",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "validationError", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A franchise with the same name already exists",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "duplicateName", value = OpenApiExamples.DUPLICATE_FRANCHISE))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected persistence or infrastructure failure",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "unexpectedError", value = OpenApiExamples.UNEXPECTED_ERROR)))
            })
    Mono<ServerResponse> create(final ServerRequest request);

    @Operation(
            operationId = "getFranchiseById",
            tags = {"Franchises"},
            summary = "Get a franchise by id",
            description = "Returns the franchise that matches the given identifier.",
            parameters = @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "Franchise unique identifier",
                    schema = @Schema(
                            type = "string",
                            format = "uuid",
                            example = "550e8400-e29b-41d4-a716-446655440000")),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Franchise found",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = FranchiseResponseDTO.class),
                                    examples = @ExampleObject(name = "found", value = OpenApiExamples.FRANCHISE))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The identifier is missing or syntactically invalid",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "invalidId", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No franchise exists for the given identifier",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "notFound", value = OpenApiExamples.FRANCHISE_NOT_FOUND))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected persistence or infrastructure failure",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "unexpectedError", value = OpenApiExamples.UNEXPECTED_ERROR)))
            })
    Mono<ServerResponse> findById(final ServerRequest request);
}
