package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch;

import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi.BranchApi;

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
public class BranchRouter {

    private static final String BRANCHES = "/v1/branches";
    private static final String BRANCH_BY_ID = "/v1/branches/{id}";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = BRANCHES,
                    method = RequestMethod.POST,
                    consumes = JSON,
                    produces = JSON,
                    beanClass = BranchApi.class,
                    beanMethod = "add"),
            @RouterOperation(
                    path = BRANCH_BY_ID,
                    method = RequestMethod.PATCH,
                    consumes = JSON,
                    produces = JSON,
                    beanClass = BranchApi.class,
                    beanMethod = "update")
    })
    public RouterFunction<ServerResponse> branchRoutes(final BranchHandler handler) {
        return route()
                .POST(BRANCHES, handler::add)
                .PATCH(BRANCH_BY_ID, handler::update)
                .build();
    }
}
