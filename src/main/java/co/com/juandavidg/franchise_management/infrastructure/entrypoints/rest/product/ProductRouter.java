package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.ProductApi;

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
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = PRODUCTS,
                    method = RequestMethod.POST,
                    consumes = JSON,
                    produces = JSON,
                    beanClass = ProductApi.class,
                    beanMethod = "add")
    })
    public RouterFunction<ServerResponse> productRoutes(final ProductHandler handler) {
        return route()
                .POST(PRODUCTS, handler::add)
                .build();
    }
}
