package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.model.exceptions.TechnicalException;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.instances.dynamodb.record-exceptions[0]=java.util.concurrent.TimeoutException",
        "resilience4j.retry.instances.dynamodb.retry-exceptions[0]=java.util.concurrent.TimeoutException"
})
@ActiveProfiles("local")
@Testcontainers
class FranchiseDynamoResilienceIT {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.4.0");
    private static final DockerImageName TOXIPROXY_IMAGE =
            DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.11.0");
    private static final int PROXY_PORT = 8666;
    private static final String INSTANCE = "dynamodb";
    private static final Network NETWORK = Network.newNetwork();

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("localstack")
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Container
    static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer(TOXIPROXY_IMAGE)
            .withNetwork(NETWORK);

    static Proxy dynamoProxy;

    @Autowired
    private FranchiseRepositoryPort repository;

    @Autowired
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    private DynamoDbProperties properties;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @DynamicPropertySource
    static void registerLocalStack(final DynamicPropertyRegistry registry) {
        dynamoProxy = createDynamoProxy();
        registry.add("aws.dynamodb.endpoint",
                () -> "http://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(PROXY_PORT));
        registry.add("aws.dynamodb.region", localStack::getRegion);
        registry.add("aws.access-key-id", localStack::getAccessKey);
        registry.add("aws.secret-access-key", localStack::getSecretKey);
        registry.add("aws.dynamodb.table-name", () -> "FranchiseManagement");
        registry.add("aws.dynamodb.resilience-instance", () -> INSTANCE);
        registry.add("resilience4j.timelimiter.instances.dynamodb.timeout-duration", () -> "800ms");
        registry.add("resilience4j.retry.instances.dynamodb.max-attempts", () -> 4);
        registry.add("resilience4j.retry.instances.dynamodb.wait-duration", () -> "80ms");
        registry.add("resilience4j.retry.instances.dynamodb.enable-exponential-backoff", () -> false);
        registry.add("resilience4j.retry.instances.dynamodb.enable-randomized-wait", () -> false);
        registry.add("resilience4j.circuitbreaker.instances.dynamodb.sliding-window-size", () -> 10);
        registry.add("resilience4j.circuitbreaker.instances.dynamodb.minimum-number-of-calls", () -> 5);
        registry.add("resilience4j.circuitbreaker.instances.dynamodb.failure-rate-threshold", () -> 50);
        registry.add("resilience4j.circuitbreaker.instances.dynamodb.wait-duration-in-open-state", () -> "1m");
    }

    @BeforeEach
    void setUp() {
        restoreProxyAndBreaker();
        StepVerifier.create(ensureTable())
                .verifyComplete();
    }

    @AfterEach
    void tearDown() {
        restoreProxyAndBreaker();
    }

    @Test
    void shouldSaveAndFindThroughProxy() {
        // ARRANGE
        final Franchise franchise = franchise("Proxy Health");

        // ACT & ASSERT
        StepVerifier.create(repository.save(franchise).then(repository.findById(franchise.getId())))
                .expectNextMatches(found -> franchise.getId().equals(found.getId()))
                .verifyComplete();
    }

    @Test
    void shouldTimeoutWhenDynamoStaysSlow() {
        // ARRANGE
        addLatency(2_500);

        // ACT & ASSERT
        StepVerifier.create(repository.findById("slow-" + UUID.randomUUID()))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.TIMEOUT))
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void shouldRetryWhenDynamoRecoversBeforeLastAttempt() {
        // ARRANGE
        final Franchise franchise = franchise("Retry Recovery");
        final long successfulRetriesBefore = successfulRetriesWithAttempt();

        // ACT & ASSERT
        StepVerifier.create(
                        repository.save(franchise)
                                .flatMap(saved -> Mono.fromRunnable(() -> addLatency(2_500))
                                        .then(repository.findById(saved.getId())
                                                .mergeWith(Mono.delay(Duration.ofMillis(850))
                                                        .doOnSuccess(tick -> clearToxics())
                                                        .then(Mono.empty()))
                                                .next())))
                .expectNextMatches(found -> franchise.getId().equals(found.getId()))
                .verifyComplete();
        Assertions.assertTrue(successfulRetriesWithAttempt() > successfulRetriesBefore);
    }

    @Test
    void shouldRejectCallsWhenCircuitIsOpen() {
        // ARRANGE
        addLatency(2_500);

        // ACT & ASSERT
        StepVerifier.create(
                        repository.findById("down-1")
                                .onErrorResume(error -> Mono.empty())
                                .then(repository.findById("down-2")
                                        .onErrorResume(error -> Mono.empty()))
                                .then(Mono.fromRunnable(this::clearToxics))
                                .then(repository.findById("after-open-" + UUID.randomUUID())))
                .expectErrorMatches(error -> isTechnical(error, ErrorCode.SERVICE_UNAVAILABLE))
                .verify(Duration.ofSeconds(15));
    }

    @SneakyThrows
    private static Proxy createDynamoProxy() {
        return new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort())
                .createProxy("dynamodb", "0.0.0.0:" + PROXY_PORT, "localstack:4566");
    }

    private void restoreProxyAndBreaker() {
        clearToxics();
        enableProxy();
        circuitBreakerRegistry.circuitBreaker(INSTANCE).reset();
    }

    @SneakyThrows
    private void addLatency(final long millis) {
        dynamoProxy.toxics().latency("slow-dynamo", ToxicDirection.DOWNSTREAM, millis);
    }

    @SneakyThrows
    private void enableProxy() {
        dynamoProxy.enable();
    }

    @SneakyThrows
    private void clearToxics() {
        for (final Toxic toxic : dynamoProxy.toxics().getAll()) {
            toxic.remove();
        }
    }

    private long successfulRetriesWithAttempt() {
        return retryRegistry.retry(INSTANCE).getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt();
    }

    private Mono<Void> ensureTable() {
        return Mono.fromFuture(() -> dynamoDbAsyncClient.createTable(CreateTableRequest.builder()
                        .tableName(properties.tableName())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("PK")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("SK")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .keySchema(
                                KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build()))
                .then()
                .onErrorResume(ResourceInUseException.class, error -> Mono.empty());
    }

    private static boolean isTechnical(final Throwable error, final ErrorCode code) {
        return error instanceof TechnicalException technicalException
                && code == technicalException.getCode();
    }

    private static Franchise franchise(final String name) {
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Franchise.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
