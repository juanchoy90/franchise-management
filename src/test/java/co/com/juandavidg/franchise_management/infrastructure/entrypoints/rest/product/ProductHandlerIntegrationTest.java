package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product;

import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto.BranchResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.FranchiseResponseDTO;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto.ProductResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
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

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
@Testcontainers
class ProductHandlerIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.4.0");

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private WebTestClient client;

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
    void shouldAddProductToExistingBranch() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("McDonald's"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Downtown");

        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), branch.id(), "Fries", 10))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.franchiseId").isEqualTo(franchise.id())
                .jsonPath("$.branchId").isEqualTo(branch.id())
                .jsonPath("$.name").isEqualTo("Fries")
                .jsonPath("$.stock").isEqualTo(10)
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    void shouldRejectBlankProductName() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("KFC"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Airport");

        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), branch.id(), "", 10))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.message").isEqualTo("name: must not be blank")
                .jsonPath("$.path").exists()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectNegativeStock() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Subway"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Downtown");

        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), branch.id(), "Fries", -1))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.message").isEqualTo("stock: must be greater than or equal to 0")
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectProductWhenFranchiseIsMissing() {
        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson("missing-" + UUID.randomUUID(), UUID.randomUUID().toString(), "Fries", 10))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectProductWhenBranchIsMissing() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Wendy's"));

        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), "missing-" + UUID.randomUUID(), "Fries", 10))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.BRANCH_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectDuplicateProductNameInBranch() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Popeyes"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Airport");
        addProduct(franchise.id(), branch.id(), "Fries", 10);

        // ACT & ASSERT
        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), branch.id(), "Fries", 5))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_ALREADY_EXISTS")
                .jsonPath("$.message").isEqualTo(ErrorCode.PRODUCT_ALREADY_EXISTS.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldDeleteProductAndAllowReusingName() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Burger King"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Downtown");
        final ProductResponseDTO product = addProduct(franchise.id(), branch.id(), "Fries", 10);

        // ACT & ASSERT
        client.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", franchise.id())
                        .queryParam("branchId", branch.id())
                        .build(product.id()))
                .exchange()
                .expectStatus().isNoContent();

        client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchise.id(), branch.id(), "Fries", 8))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Fries")
                .jsonPath("$.stock").isEqualTo(8);
    }

    @Test
    void shouldRejectDeleteWhenProductIsMissing() {
        // ACT & ASSERT
        client.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", UUID.randomUUID().toString())
                        .queryParam("branchId", UUID.randomUUID().toString())
                        .build(UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldApplyStockDelta() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("In-N-Out"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Airport");
        final ProductResponseDTO product = addProduct(franchise.id(), branch.id(), "Fries", 10);

        // ACT & ASSERT
        client.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", franchise.id())
                        .queryParam("branchId", branch.id())
                        .build(product.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"delta":15}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(product.id())
                .jsonPath("$.stock").isEqualTo(25)
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    void shouldRejectDeltaWhenStockWouldBeNegative() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Chipotle"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Downtown");
        final ProductResponseDTO product = addProduct(franchise.id(), branch.id(), "Fries", 10);

        // ACT & ASSERT
        client.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", franchise.id())
                        .queryParam("branchId", branch.id())
                        .build(product.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"delta":-20}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_STOCK")
                .jsonPath("$.message").isEqualTo(ErrorCode.INSUFFICIENT_STOCK.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectStockUpdateWhenProductIsMissing() {
        // ACT & ASSERT
        client.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", UUID.randomUUID().toString())
                        .queryParam("branchId", UUID.randomUUID().toString())
                        .build(UUID.randomUUID().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"delta":5}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectDeltaOutOfRange() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Five Guys"));
        final BranchResponseDTO branch = addBranch(franchise.id(), "Airport");
        final ProductResponseDTO product = addProduct(franchise.id(), branch.id(), "Fries", 10);

        // ACT & ASSERT
        client.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/products/{id}")
                        .queryParam("franchiseId", franchise.id())
                        .queryParam("branchId", branch.id())
                        .build(product.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"delta":10000001}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.message").isEqualTo("delta: must be less than or equal to 10000000")
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectStockUpdateWhenQueryParamsAreMissing() {
        // ACT & ASSERT
        client.patch()
                .uri("/v1/products/{id}", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"delta":5}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectDeleteWhenQueryParamsAreMissing() {
        // ACT & ASSERT
        client.delete()
                .uri("/v1/products/{id}", UUID.randomUUID().toString())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    private FranchiseResponseDTO createFranchise(final String name) {
        final FranchiseResponseDTO created = client.post()
                .uri("/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s"}
                        """.formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponseDTO.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(created);
    }

    private BranchResponseDTO addBranch(final String franchiseId, final String name) {
        final BranchResponseDTO created = client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"franchiseId":"%s","name":"%s"}
                        """.formatted(franchiseId, name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponseDTO.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(created);
    }

    private ProductResponseDTO addProduct(
            final String franchiseId,
            final String branchId,
            final String name,
            final int stock) {
        final ProductResponseDTO created = client.post()
                .uri("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson(franchiseId, branchId, name, stock))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponseDTO.class)
                .returnResult()
                .getResponseBody();
        return Objects.requireNonNull(created);
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

    private static String productJson(
            final String franchiseId,
            final String branchId,
            final String name,
            final int stock) {
        return """
                {"franchiseId":"%s","branchId":"%s","name":"%s","stock":%d}
                """.formatted(franchiseId, branchId, name, stock);
    }

    private static String uniqueName(final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
