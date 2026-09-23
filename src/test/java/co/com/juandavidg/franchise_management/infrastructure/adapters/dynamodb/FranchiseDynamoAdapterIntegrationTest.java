package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class FranchiseDynamoAdapterIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.4.0");

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private FranchiseRepositoryPort repository;

    @Autowired
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    private DynamoDbProperties properties;

    @DynamicPropertySource
    static void registerLocalStack(final DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint",
                () -> localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        registry.add("aws.dynamodb.region", localStack::getRegion);
        registry.add("aws.access-key-id", localStack::getAccessKey);
        registry.add("aws.secret-access-key", localStack::getSecretKey);
        registry.add("aws.dynamodb.table-name", () -> "FranchiseManagement");
        registry.add("aws.dynamodb.resilience-instance", () -> "dynamodb");
    }

    @BeforeEach
    void createTable() {
        StepVerifier.create(ensureTable())
                .verifyComplete();
    }

    @Test
    void shouldSaveAndFindFranchise() {
        // ARRANGE
        final Franchise franchise = franchise("McDonald's");

        // ACT & ASSERT
        StepVerifier.create(repository.save(franchise).then(repository.findById(franchise.getId())))
                .expectNextMatches(found ->
                        franchise.getId().equals(found.getId()) && "McDonald's".equals(found.getName()))
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateFranchiseId() {
        // ARRANGE
        final Franchise franchise = franchise("KFC");

        // ACT & ASSERT
        StepVerifier.create(repository.save(franchise).then(repository.save(franchise)))
                .expectErrorMatches(this::isAlreadyExists)
                .verify();
    }

    @Test
    void shouldRejectDuplicateFranchiseName() {
        // ARRANGE
        final Franchise first = franchise("Burger King");
        final Franchise second = franchise("Burger King");

        // ACT & ASSERT
        StepVerifier.create(repository.save(first).then(repository.save(second)))
                .expectErrorMatches(this::isAlreadyExists)
                .verify();
    }

    @Test
    void shouldReturnEmptyWhenFranchiseDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.findById("missing-" + UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void shouldDetectExistingFranchiseName() {
        // ARRANGE
        final Franchise franchise = franchise("Subway");

        // ACT & ASSERT
        StepVerifier.create(repository.save(franchise).then(repository.existsByName("subway")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenNameDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.existsByName("unknown-" + UUID.randomUUID()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldUpdateFranchiseName() {
        // ARRANGE
        final Franchise franchise = franchise("Wendy's");

        // ACT & ASSERT
        StepVerifier.create(
                        repository.save(franchise)
                                .then(repository.updateName(franchise.getId(), "Popeyes"))
                                .then(repository.findById(franchise.getId())))
                .expectNextMatches(updated -> "Popeyes".equals(updated.getName()))
                .verifyComplete();

        StepVerifier.create(repository.existsByName("Wendy's"))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.existsByName("Popeyes"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenUpdatingMissingFranchise() {
        // ACT & ASSERT
        StepVerifier.create(repository.updateName("missing-" + UUID.randomUUID(), "New Name"))
                .verifyComplete();
    }

    @Test
    void shouldRejectRenameToExistingName() {
        // ARRANGE
        final Franchise first = franchise("Taco Bell");
        final Franchise second = franchise("Pizza Hut");

        // ACT & ASSERT
        StepVerifier.create(
                        repository.save(first)
                                .then(repository.save(second))
                                .then(repository.updateName(first.getId(), "Pizza Hut")))
                .expectErrorMatches(this::isAlreadyExists)
                .verify();
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

    private boolean isAlreadyExists(final Throwable error) {
        return error instanceof BusinessException businessException
                && ErrorCode.FRANCHISE_ALREADY_EXISTS == businessException.getCode();
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
