package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.FranchiseApi;

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
public class FranchiseRouter {

    private static final String FRANCHISES = "/v1/franchises";
    private static final String FRANCHISE_BY_ID = "/v1/franchises/{id}";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = FRANCHISES,
                    method = RequestMethod.POST,
                    consumes = JSON,
                    produces = JSON,
                    beanClass = FranchiseApi.class,
                    beanMethod = "create"),
            @RouterOperation(
                    path = FRANCHISE_BY_ID,
                    method = RequestMethod.GET,
                    produces = JSON,
                    beanClass = FranchiseApi.class,
                    beanMethod = "findById")
    })
    public RouterFunction<ServerResponse> franchiseRoutes(final FranchiseHandler handler) {
        return route()
                .POST(FRANCHISES, handler::create)
                .GET(FRANCHISE_BY_ID, handler::findById)
                .build();
    }
}
