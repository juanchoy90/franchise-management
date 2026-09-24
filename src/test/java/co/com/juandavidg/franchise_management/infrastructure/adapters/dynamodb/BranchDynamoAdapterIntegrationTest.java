package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
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
class BranchDynamoAdapterIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.4.0");

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private BranchRepositoryPort repository;

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
    void shouldSaveBranchAndDetectName() {
        // ARRANGE
        final Branch branch = branch(UUID.randomUUID().toString(), "Downtown");

        // ACT & ASSERT
        StepVerifier.create(repository.save(branch)
                        .then(repository.existsByFranchiseIdAndName(branch.getFranchiseId(), "Downtown")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateBranchName() {
        // ARRANGE
        final String franchiseId = UUID.randomUUID().toString();
        final Branch first = branch(franchiseId, "Airport");
        final Branch second = branch(franchiseId, "Airport");

        // ACT & ASSERT
        StepVerifier.create(repository.save(first).then(repository.save(second)))
                .expectErrorMatches(this::isAlreadyExists)
                .verify();
    }

    @Test
    void shouldReturnFalseWhenBranchDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.existsByFranchiseIdAndName(
                        UUID.randomUUID().toString(), "unknown-" + UUID.randomUUID()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldAllowSameBranchNameInDifferentFranchises() {
        // ARRANGE
        final Branch first = branch(UUID.randomUUID().toString(), "Downtown");
        final Branch second = branch(UUID.randomUUID().toString(), "Downtown");

        // ACT & ASSERT
        StepVerifier.create(repository.save(first).then(repository.save(second)))
                .expectNextMatches(saved ->
                        second.getId().equals(saved.getId())
                                && second.getFranchiseId().equals(saved.getFranchiseId())
                                && "Downtown".equals(saved.getName()))
                .verifyComplete();
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

    private static Branch branch(final String franchiseId, final String name) {
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Branch.builder()
                .id(UUID.randomUUID().toString())
                .franchiseId(franchiseId)
                .name(name)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
