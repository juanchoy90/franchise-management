package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.model.exceptions.BusinessException;
import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
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
class ProductDynamoAdapterIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.4.0");

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private ProductRepositoryPort repository;

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
    void shouldSaveProductAndDetectName() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product)
                        .then(repository.existsByFranchiseIdAndBranchIdAndName(
                                product.getFranchiseId(), product.getBranchId(), "Fries")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateProductId() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product).then(repository.save(product)))
                .expectErrorMatches(this::isAlreadyExists)
                .verify();
    }

    @Test
    void shouldReturnFalseWhenProductDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.existsByFranchiseIdAndBranchIdAndName(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        "unknown-" + UUID.randomUUID()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldAllowSameProductNameInDifferentBranches() {
        // ARRANGE
        final String franchiseId = UUID.randomUUID().toString();
        final Product first = product(franchiseId, UUID.randomUUID().toString(), "Fries");
        final Product second = product(franchiseId, UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(first).then(repository.save(second)))
                .expectNextMatches(saved ->
                        second.getId().equals(saved.getId())
                                && second.getBranchId().equals(saved.getBranchId())
                                && "Fries".equals(saved.getName()))
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

    private static Product product(final String franchiseId, final String branchId, final String name) {
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Product.builder()
                .id(UUID.randomUUID().toString())
                .franchiseId(franchiseId)
                .branchId(branchId)
                .name(name)
                .stock(10)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
