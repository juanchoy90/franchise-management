package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception.ErrorResponse;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.ProductApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ProductRouter {

    private static final String PRODUCTS = "/v1/products";
    private static final String PRODUCT_BY_ID = "/v1/products/{id}";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = PRODUCTS,
                    method = RequestMethod.POST,
                    consumes = JSON,
                    produces = JSON,
                    beanClass = ProductApi.class,
                    beanMethod = "add"),
            @RouterOperation(
                    path = PRODUCT_BY_ID,
                    method = RequestMethod.DELETE,
                    produces = JSON,
                    beanClass = ProductHandler.class,
                    beanMethod = "delete",
                    operation = @Operation(
                            operationId = "deleteProduct",
                            tags = {"Products"},
                            summary = "Delete a product from a branch",
                            description = "Removes the product and releases its name so the same name can be reused in that branch.",
                            parameters = {
                                    @Parameter(
                                            name = "id",
                                            in = ParameterIn.PATH,
                                            required = true,
                                            description = "Product unique identifier"),
                                    @Parameter(
                                            name = "franchiseId",
                                            in = ParameterIn.QUERY,
                                            required = true,
                                            description = "Franchise that owns the product"),
                                    @Parameter(
                                            name = "branchId",
                                            in = ParameterIn.QUERY,
                                            required = true,
                                            description = "Branch that owns the product")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Product deleted"),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "franchiseId or branchId is missing",
                                            content = @Content(
                                                    mediaType = JSON,
                                                    schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(
                                            responseCode = "404",
                                            description = "The product does not exist",
                                            content = @Content(
                                                    mediaType = JSON,
                                                    schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Unexpected persistence or infrastructure failure",
                                            content = @Content(
                                                    mediaType = JSON,
                                                    schema = @Schema(implementation = ErrorResponse.class)))
                            }))
    })
    public RouterFunction<ServerResponse> productRoutes(final ProductHandler handler) {
        return route()
                .POST(PRODUCTS, handler::add)
                .DELETE(PRODUCT_BY_ID, handler::delete)
                .build();
    }
}
