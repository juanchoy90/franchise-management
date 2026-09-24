package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchise Management API")
                        .description("Reactive API to register and query franchises.")
                        .version("v1"));
    }
}
