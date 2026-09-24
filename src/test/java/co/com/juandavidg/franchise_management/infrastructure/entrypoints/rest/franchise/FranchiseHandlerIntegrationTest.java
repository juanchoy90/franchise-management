package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise;

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
class FranchiseHandlerIntegrationTest {

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
    void shouldCreateFranchise() {
        // ARRANGE
        final String name = uniqueName("McDonald's");

        // ACT & ASSERT
        client.post()
                .uri("/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    void shouldRejectBlankFranchiseName() {
        // ACT & ASSERT
        client.post()
                .uri("/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":""}
                        """)
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
    void shouldRejectDuplicateFranchise() {
        // ARRANGE
        final String name = uniqueName("KFC");
        createFranchise(name);

        // ACT & ASSERT
        client.post()
                .uri("/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson(name))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_ALREADY_EXISTS")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_ALREADY_EXISTS.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldGetFranchiseById() {
        // ARRANGE
        final String name = uniqueName("Subway");
        final FranchiseResponseDTO created = createFranchise(name);

        // ACT & ASSERT
        client.get()
                .uri("/v1/franchises/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id())
                .jsonPath("$.name").isEqualTo(name);
    }

    @Test
    void shouldRejectMissingFranchise() {
        // ACT & ASSERT
        client.get()
                .uri("/v1/franchises/{id}", "missing-" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldUpdateFranchiseName() {
        // ARRANGE
        final FranchiseResponseDTO created = createFranchise(uniqueName("Wendy's"));
        final String newName = uniqueName("Popeyes");

        // ACT & ASSERT
        client.patch()
                .uri("/v1/franchises/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson(newName))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id())
                .jsonPath("$.name").isEqualTo(newName);
    }

    @Test
    void shouldRejectUpdateWhenFranchiseIsMissing() {
        // ACT & ASSERT
        client.patch()
                .uri("/v1/franchises/{id}", "missing-" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson("Popeyes"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectUpdateWhenNameAlreadyExists() {
        // ARRANGE
        final String existingName = uniqueName("Pizza Hut");
        createFranchise(existingName);
        final FranchiseResponseDTO target = createFranchise(uniqueName("Taco Bell"));

        // ACT & ASSERT
        client.patch()
                .uri("/v1/franchises/{id}", target.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson(existingName))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_ALREADY_EXISTS")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_ALREADY_EXISTS.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectBlankNameOnUpdate() {
        // ARRANGE
        final FranchiseResponseDTO created = createFranchise(uniqueName("Arby's"));

        // ACT & ASSERT
        client.patch()
                .uri("/v1/franchises/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.message").isEqualTo("name: must not be blank")
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldListTheHighestStockProductOfEachBranch() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("In-N-Out"));
        final BranchResponseDTO downtown = addBranch(franchise.id(), "Downtown");
        final BranchResponseDTO airport = addBranch(franchise.id(), "Airport");
        addProduct(franchise.id(), downtown.id(), "Fries", 10);
        final ProductResponseDTO burger = addProduct(franchise.id(), downtown.id(), "Burger", 40);
        addProduct(franchise.id(), airport.id(), "Soda", 5);

        // ACT & ASSERT
        client.get()
                .uri("/v1/franchises/{id}/products/top-stock", franchise.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[?(@.branchName=='Downtown')].branchId").value(ids ->
                        assertThat(ids.toString()).contains(downtown.id()))
                .jsonPath("$[?(@.branchName=='Downtown')].product.id").value(ids ->
                        assertThat(ids.toString()).contains(burger.id()))
                .jsonPath("$[?(@.branchName=='Downtown')].product.stock").value(stocks ->
                        assertThat(stocks.toString()).contains("40"))
                .jsonPath("$[?(@.branchName=='Airport')].product.stock").value(stocks ->
                        assertThat(stocks.toString()).contains("5"));
    }

    @Test
    void shouldIncludeBranchesWithoutProducts() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Shake Shack"));
        final BranchResponseDTO empty = addBranch(franchise.id(), "Empty");

        // ACT & ASSERT
        client.get()
                .uri("/v1/franchises/{id}/products/top-stock", franchise.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].branchId").isEqualTo(empty.id())
                .jsonPath("$[0].product").value(product -> assertThat(product).isNull());
    }

    @Test
    void shouldRejectTopStockWhenFranchiseIsMissing() {
        // ACT & ASSERT
        client.get()
                .uri("/v1/franchises/{id}/products/top-stock", UUID.randomUUID().toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    private FranchiseResponseDTO createFranchise(final String name) {
        final FranchiseResponseDTO created = client.post()
                .uri("/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(franchiseJson(name))
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
                .bodyValue("""
                        {"franchiseId":"%s","branchId":"%s","name":"%s","stock":%d}
                        """.formatted(franchiseId, branchId, name, stock))
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
                                        .nonKeyAttributes("name", "nameKey", "id", "branchId", "franchiseId")
                                        .build())
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build()))
                .then()
                .onErrorResume(ResourceInUseException.class, error -> Mono.empty());
    }

    private static String franchiseJson(final String name) {
        return """
                {"name":"%s"}
                """.formatted(name);
    }

    private static String uniqueName(final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
