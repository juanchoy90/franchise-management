package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch;

import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto.FranchiseResponseDTO;

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
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
@Testcontainers
class BranchHandlerIntegrationTest {

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
    void shouldAddBranchToExistingFranchise() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("McDonald's"));

        // ACT & ASSERT
        client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchJson(franchise.id(), "Downtown"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.franchiseId").isEqualTo(franchise.id())
                .jsonPath("$.name").isEqualTo("Downtown")
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    void shouldRejectBlankBranchName() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("KFC"));

        // ACT & ASSERT
        client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchJson(franchise.id(), ""))
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
    void shouldRejectBranchWhenFranchiseIsMissing() {
        // ACT & ASSERT
        client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchJson("missing-" + UUID.randomUUID(), "Downtown"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND.getMessage())
                .jsonPath("$.traceId").value(traceId -> assertThat(traceId).isNotEqualTo("n/a"));
    }

    @Test
    void shouldRejectDuplicateBranchNameInFranchise() {
        // ARRANGE
        final FranchiseResponseDTO franchise = createFranchise(uniqueName("Subway"));
        addBranch(franchise.id(), "Airport");

        // ACT & ASSERT
        client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchJson(franchise.id(), "Airport"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("BRANCH_ALREADY_EXISTS")
                .jsonPath("$.message").isEqualTo(ErrorCode.BRANCH_ALREADY_EXISTS.getMessage())
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

    private void addBranch(final String franchiseId, final String name) {
        client.post()
                .uri("/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(branchJson(franchiseId, name))
                .exchange()
                .expectStatus().isCreated();
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

    private static String branchJson(final String franchiseId, final String name) {
        return """
                {"franchiseId":"%s","name":"%s"}
                """.formatted(franchiseId, name);
    }

    private static String uniqueName(final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
