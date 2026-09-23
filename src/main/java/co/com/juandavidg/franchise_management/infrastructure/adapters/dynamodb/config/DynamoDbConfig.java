package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
@RequiredArgsConstructor
public class DynamoDbConfig {
    
    private final DynamoDbProperties properties;
    
    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        log.info("Initializing DynamoDB Async Client with Netty NIO");
        log.debug("DynamoDB Configuration - Table: {}, Region: {}, Endpoint: {}", 
                  properties.tableName(), 
                  properties.region(), 
                  properties.endpoint());
        
        final NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(properties.maxConcurrency())
                .connectionAcquisitionTimeout(Duration.ofMillis(properties.connectionAcquisitionTimeout()))
                .connectionTimeout(Duration.ofMillis(properties.connectionTimeout()))
                .readTimeout(Duration.ofMillis(properties.readTimeout()))
                .writeTimeout(Duration.ofMillis(properties.readTimeout()));
        
        final var clientBuilder = DynamoDbAsyncClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(httpClientBuilder.build());
        
        if (properties.endpoint() != null && !properties.endpoint().isEmpty()) {
            log.info("Using custom DynamoDB endpoint: {}", properties.endpoint());
            clientBuilder.endpointOverride(URI.create(properties.endpoint()));
        }
        
        final DynamoDbAsyncClient client = clientBuilder.build();
        log.info("DynamoDB Async Client initialized successfully");
        
        return client;
    }
    
    @Bean
    public String dynamoDbTableName() {
        return properties.tableName();
    }
}
