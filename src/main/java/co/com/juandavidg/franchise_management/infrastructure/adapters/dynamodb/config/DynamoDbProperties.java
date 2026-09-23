package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.dynamodb")
public record DynamoDbProperties(
    String tableName,
    String endpoint,
    String region,
    Integer maxConcurrency,
    Long connectionAcquisitionTimeout,
    Long connectionTimeout,
    Long readTimeout,
    String resilienceInstance
) {
}
