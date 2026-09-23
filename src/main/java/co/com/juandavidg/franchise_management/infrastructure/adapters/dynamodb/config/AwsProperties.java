package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    String accessKeyId,
    String secretAccessKey
) {
}
