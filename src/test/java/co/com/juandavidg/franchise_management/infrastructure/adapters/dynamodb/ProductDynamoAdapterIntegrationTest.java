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
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
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
    void shouldApplyStockDeltaAndReturnUpdatedItem() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product)
                        .then(repository.updateStock(
                                product.getFranchiseId(), product.getBranchId(), product.getId(), 15)))
                .expectNextMatches(updated ->
                        product.getId().equals(updated.getId())
                                && Integer.valueOf(25).equals(updated.getStock()))
                .verifyComplete();
    }

    @Test
    void shouldRejectInsufficientStockOnDelta() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product)
                        .then(repository.updateStock(
                                product.getFranchiseId(), product.getBranchId(), product.getId(), -20)))
                .expectErrorMatches(this::isInsufficientStock)
                .verify();
    }

    @Test
    void shouldRejectStockDeltaWhenProductDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.updateStock(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        5))
                .expectErrorMatches(this::isProductNotFound)
                .verify();
    }

    @Test
    void shouldCompleteDeleteWhenProductDoesNotExist() {
        // ACT & ASSERT
        StepVerifier.create(repository.delete(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString()))
                .verifyComplete();
    }

    @Test
    void shouldDeleteProductAndReleaseName() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product)
                        .then(repository.delete(product.getFranchiseId(), product.getBranchId(), product.getId()))
                        .then(repository.existsByFranchiseIdAndBranchIdAndName(
                                product.getFranchiseId(), product.getBranchId(), "Fries")))
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

    @Test
    void shouldReturnTheProductWithTheHighestStock() {
        // ARRANGE
        final String franchiseId = UUID.randomUUID().toString();
        final String branchId = UUID.randomUUID().toString();
        final Product low = product(franchiseId, branchId, "Fries", 10);
        final Product high = product(franchiseId, branchId, "Burger", 40);
        final Product otherBranch = product(franchiseId, UUID.randomUUID().toString(), "Soda", 99);

        // ACT & ASSERT
        StepVerifier.create(repository.save(low)
                        .then(repository.save(high))
                        .then(repository.save(otherBranch))
                        .then(repository.findTopStock(franchiseId, branchId)))
                .expectNextMatches(top ->
                        high.getId().equals(top.getId()) && Integer.valueOf(40).equals(top.getStock()))
                .verifyComplete();
    }

    @Test
    void shouldRenameProductAndReleaseOldName() {
        // ARRANGE
        final Product product = product(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Fries");

        // ACT & ASSERT
        StepVerifier.create(repository.save(product)
                        .then(repository.updateName(
                                product.getFranchiseId(), product.getBranchId(), product.getId(), "Burger"))
                        .flatMap(updated -> repository.existsByFranchiseIdAndBranchIdAndName(
                                product.getFranchiseId(), product.getBranchId(), "Fries")
                                .map(oldTaken -> updated.getName() + ":" + oldTaken)))
                .expectNext("Burger:false")
                .verifyComplete();
    }

    @Test
    void shouldRejectRenameWhenProductNameAlreadyExists() {
        // ARRANGE
        final String franchiseId = UUID.randomUUID().toString();
        final String branchId = UUID.randomUUID().toString();
        final Product fries = product(franchiseId, branchId, "Fries");
        final Product burger = product(franchiseId, branchId, "Burger");

        // ACT & ASSERT
        StepVerifier.create(repository.save(fries)
                        .then(repository.save(burger))
                        .then(repository.updateName(franchiseId, branchId, fries.getId(), "Burger")))
                .expectErrorMatches(this::isProductAlreadyExists)
                .verify();
    }

    @Test
    void shouldCompleteEmptyWhenRenamingMissingProduct() {
        // ACT & ASSERT
        StepVerifier.create(repository.updateName(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        "Burger"))
                .verifyComplete();
    }

    @Test
    void shouldCompleteEmptyWhenBranchHasNoProducts() {
        // ACT & ASSERT
        StepVerifier.create(repository.findTopStock(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
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
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("GSI1PK")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("stock")
                                        .attributeType(ScalarAttributeType.N)
                                        .build())
                        .keySchema(
                                KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
                        .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                .indexName("GSI1")
                                .keySchema(
                                        KeySchemaElement.builder().attributeName("GSI1PK").keyType(KeyType.HASH).build(),
                                        KeySchemaElement.builder().attributeName("stock").keyType(KeyType.RANGE).build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes("name", "id", "branchId", "franchiseId")
                                        .build())
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build()))
                .then()
                .onErrorResume(ResourceInUseException.class, error -> Mono.empty());
    }

    private boolean isAlreadyExists(final Throwable error) {
        return error instanceof BusinessException businessException
                && ErrorCode.FRANCHISE_ALREADY_EXISTS == businessException.getCode();
    }

    private boolean isProductAlreadyExists(final Throwable error) {
        return error instanceof BusinessException businessException
                && ErrorCode.PRODUCT_ALREADY_EXISTS == businessException.getCode();
    }

    private boolean isProductNotFound(final Throwable error) {
        return error instanceof BusinessException businessException
                && ErrorCode.PRODUCT_NOT_FOUND == businessException.getCode();
    }

    private boolean isInsufficientStock(final Throwable error) {
        return error instanceof BusinessException businessException
                && ErrorCode.INSUFFICIENT_STOCK == businessException.getCode();
    }

    private static Product product(final String franchiseId, final String branchId, final String name) {
        return product(franchiseId, branchId, name, 10);
    }

    private static Product product(
            final String franchiseId,
            final String branchId,
            final String name,
            final int stock) {
        final Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Product.builder()
                .id(UUID.randomUUID().toString())
                .franchiseId(franchiseId)
                .branchId(branchId)
                .name(name)
                .stock(stock)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
