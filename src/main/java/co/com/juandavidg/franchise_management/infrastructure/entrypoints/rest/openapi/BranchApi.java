package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.AddBranchDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.BranchResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception.ErrorResponse;

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

public interface BranchApi {

    String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Operation(
            operationId = "addBranch",
            tags = {"Branches"},
            summary = "Add a branch to a franchise",
            description = "Registers a new branch under an existing franchise. The branch name must be unique within that franchise.",
            requestBody = @RequestBody(
                    required = true,
                    description = "Branch to add",
                    content = @Content(
                            mediaType = JSON,
                            schema = @Schema(implementation = AddBranchDTO.class),
                            examples = @ExampleObject(
                                    name = "addBranch",
                                    summary = "Valid branch",
                                    value = OpenApiExamples.CREATE_BRANCH))),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Branch created",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = BranchResponseDTO.class),
                                    examples = @ExampleObject(name = "created", value = OpenApiExamples.BRANCH))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request body failed syntactic validation",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "validationError", value = OpenApiExamples.VALIDATION_ERROR))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No franchise exists for the given identifier",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "franchiseNotFound", value = OpenApiExamples.FRANCHISE_NOT_FOUND))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A branch with the same name already exists in the franchise",
                            content = @Content(
                                    mediaType = JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "duplicateName", value = OpenApiExamples.DUPLICATE_BRANCH))),
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
