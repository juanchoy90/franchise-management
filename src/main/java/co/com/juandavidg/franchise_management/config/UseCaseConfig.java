package co.com.juandavidg.franchise_management.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackages = "co.com.juandavidg.franchise_management.domain.usecase",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*UseCaseImpl"
        ),
        useDefaultFilters = false
)
public class UseCaseConfig {
}
