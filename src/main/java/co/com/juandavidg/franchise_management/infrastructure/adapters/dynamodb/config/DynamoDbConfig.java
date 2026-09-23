package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Configuration
@EnableConfigurationProperties({DynamoDbProperties.class, AwsProperties.class})
@RequiredArgsConstructor
public class DynamoDbConfig {
    
    private final DynamoDbProperties properties;
    private final AwsProperties awsProperties;
    
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
                .credentialsProvider(resolveCredentials())
                .httpClient(httpClientBuilder.build());

        Optional.ofNullable(properties.endpoint())
                .filter(endpoint -> !endpoint.isEmpty())
                .ifPresent(endpoint -> {
                    log.info("Using custom DynamoDB endpoint: {}", endpoint);
                    clientBuilder.endpointOverride(URI.create(endpoint));
                });
        
        final DynamoDbAsyncClient client = clientBuilder.build();
        log.info("DynamoDB Async Client initialized successfully");
        
        return client;
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(final DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient)
                .build();
    }

    private AwsCredentialsProvider resolveCredentials() {
        return Optional.ofNullable(properties.endpoint())
                .filter(endpoint -> !endpoint.isEmpty())
                .<AwsCredentialsProvider>map(endpoint ->
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        awsProperties.accessKeyId(),
                                        awsProperties.secretAccessKey())))
                .orElseGet(DefaultCredentialsProvider::create);
    }
}
