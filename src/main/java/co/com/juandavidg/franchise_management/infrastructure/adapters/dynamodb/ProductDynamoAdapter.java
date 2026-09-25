package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductNameLockEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.DynamoResilienceDecorator;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Repository
public class ProductDynamoAdapter implements ProductRepositoryPort {

    private static final Expression ITEM_NOT_EXISTS = Expression.builder()
            .expression("attribute_not_exists(PK) AND attribute_not_exists(SK)")
            .build();

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final DynamoDbAsyncTable<ProductEntity> table;
    private final DynamoDbAsyncTable<ProductNameLockEntity> nameLockTable;
    private final DynamoResilienceDecorator decorator;

    public ProductDynamoAdapter(
            final DynamoDbEnhancedAsyncClient enhancedClient,
            final DynamoDbAsyncClient dynamoDbAsyncClient,
            final DynamoDbProperties properties,
            final DynamoResilienceDecorator decorator) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(ProductEntity.class));
        this.nameLockTable = enhancedClient.table(
                properties.tableName(),
                TableSchema.fromBean(ProductNameLockEntity.class));
        this.decorator = decorator;
    }

    @Override
    public Mono<Product> save(final Product product) {
        log.debug("Saving product {} in branch {}", product.getName(), product.getBranchId());
        final ProductEntity entity = ProductMapper.toEntity(product);
        final ProductNameLockEntity nameLock = ProductNameLockEntity.from(
                product.getFranchiseId(), product.getBranchId(), product.getId(), product.getName());
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, putIfAbsent(entity, ProductEntity.class))
                .addPutItem(nameLockTable, putIfAbsent(nameLock, ProductNameLockEntity.class))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(ProductMapper.toDomain(entity)),
                "saveProduct");
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndBranchIdAndName(
            final String franchiseId,
            final String branchId,
            final String name) {
        log.debug("Checking product {} in branch {} of franchise {}", name, branchId, franchiseId);
        final Key key = Key.builder()
                .partitionValue(ProductNameLockEntity.generatePk(franchiseId))
                .sortValue(ProductNameLockEntity.generateSk(branchId, name))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> nameLockTable.getItem(key)).hasElement(),
                "existsProduct");
    }

    @Override
    public Mono<Product> findById(final String franchiseId, final String branchId, final String productId) {
        log.debug("Finding product {} in branch {} of franchise {}", productId, branchId, franchiseId);
        return decorator.decorate(
                Mono.fromFuture(() -> table.getItem(productKey(franchiseId, branchId, productId)))
                        .map(ProductMapper::toDomain),
                "findProductById");
    }

    @Override
    public Mono<Void> delete(final String franchiseId, final String branchId, final String productId) {
        log.debug("Deleting product {} in branch {} of franchise {}", productId, branchId, franchiseId);
        final Key productKey = productKey(franchiseId, branchId, productId);
        return decorator.decorate(
                Mono.fromFuture(() -> table.getItem(productKey))
                        .flatMap(entity -> deleteProductAndLock(productKey, entity)),
                "deleteProduct");
    }

    @Override
    public Mono<Product> updateStock(
            final String franchiseId,
            final String branchId,
            final String productId,
            final Integer delta) {
        log.debug("Applying stock delta {} to product {} in branch {}", delta, productId, branchId);
        return decorator.decorate(
                applyStockDelta(franchiseId, branchId, productId, delta),
                "updateProductStock");
    }

    @Override
    public Mono<Product> findTopStock(final String franchiseId, final String branchId) {
        log.debug("Finding top stock product in branch {} of franchise {}", branchId, franchiseId);
        final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(ProductEntity.generateGsi1Pk(franchiseId, branchId))
                        .build()))
                .scanIndexForward(false)
                .limit(1)
                .build();
        return decorator.decorate(
                Mono.from(table.index("GSI1").query(request))
                        .filter(page -> !page.items().isEmpty())
                        .map(page -> page.items().getFirst())
                        .map(ProductMapper::toDomain),
                "findTopStock");
    }

    @Override
    public Mono<Product> updateName(
            final String franchiseId,
            final String branchId,
            final String productId,
            final String newName) {
        log.debug("Updating product {} name in branch {} -> {}", productId, branchId, newName);
        return findById(franchiseId, branchId, productId)
                .flatMap(existing -> persistName(existing, newName));
    }

    private Mono<Product> persistName(final Product existing, final String newName) {
        final Product updated = existing.toBuilder()
                .name(newName)
                .updatedAt(Instant.now())
                .build();
        return Mono.just(updated)
                .filter(product -> ProductNameLockEntity.generateSk(existing.getBranchId(), existing.getName())
                        .equals(ProductNameLockEntity.generateSk(existing.getBranchId(), newName)))
                .flatMap(this::overwriteMetadata)
                .switchIfEmpty(persistRenamedProduct(existing.getName(), updated));
    }

    private Mono<Product> persistRenamedProduct(final String existingName, final Product updated) {
        final ProductEntity entity = ProductMapper.toEntity(updated);
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, entity)
                .addPutItem(nameLockTable, putIfAbsent(
                        ProductNameLockEntity.from(
                                updated.getFranchiseId(), updated.getBranchId(), updated.getId(), updated.getName()),
                        ProductNameLockEntity.class))
                .addDeleteItem(nameLockTable, Key.builder()
                        .partitionValue(ProductNameLockEntity.generatePk(updated.getFranchiseId()))
                        .sortValue(ProductNameLockEntity.generateSk(updated.getBranchId(), existingName))
                        .build())
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(ProductMapper.toDomain(entity)),
                "updateProductName");
    }

    private Mono<Product> overwriteMetadata(final Product product) {
        final ProductEntity entity = ProductMapper.toEntity(product);
        return decorator.decorate(
                Mono.fromFuture(() -> table.putItem(entity))
                        .thenReturn(ProductMapper.toDomain(entity)),
                "updateProductName");
    }

    private Mono<Void> deleteProductAndLock(final Key productKey, final ProductEntity entity) {
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addDeleteItem(table, TransactDeleteItemEnhancedRequest.builder()
                        .key(productKey)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(SK) AND nameKey = :nk")
                                .putExpressionValue(":nk", AttributeValue.fromS(entity.getNameKey()))
                                .build())
                        .build())
                .addDeleteItem(nameLockTable, lockKey(entity))
                .build();
        return Mono.fromFuture(() -> enhancedClient.transactWriteItems(request)).then();
    }

    private Mono<Product> applyStockDelta(
            final String franchiseId,
            final String branchId,
            final String productId,
            final Integer delta) {
        final UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(Map.of(
                        "PK", AttributeValue.fromS(ProductEntity.generatePk(franchiseId)),
                        "SK", AttributeValue.fromS(ProductEntity.generateSk(branchId, productId))))
                .updateExpression("SET stock = stock + :delta, updatedAt = :updatedAt")
                .conditionExpression("attribute_exists(SK) AND stock >= :minStock")
                .expressionAttributeValues(Map.of(
                        ":delta", AttributeValue.fromN(delta.toString()),
                        ":minStock", AttributeValue.fromN(Integer.toString(Math.max(0, -delta))),
                        ":updatedAt", AttributeValue.fromS(Instant.now().toString())))
                .returnValues(ReturnValue.ALL_NEW)
                .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                .build();
        return Mono.fromFuture(() -> dynamoDbAsyncClient.updateItem(request))
                .map(response -> ProductMapper.toDomain(table.tableSchema().mapToItem(response.attributes())));
    }

    private static Key productKey(final String franchiseId, final String branchId, final String productId) {
        return Key.builder()
                .partitionValue(ProductEntity.generatePk(franchiseId))
                .sortValue(ProductEntity.generateSk(branchId, productId))
                .build();
    }

    private static Key lockKey(final ProductEntity entity) {
        return Key.builder()
                .partitionValue(ProductNameLockEntity.generatePk(entity.getFranchiseId()))
                .sortValue(ProductNameLockEntity.generateSk(entity.getBranchId(), entity.getName()))
                .build();
    }

    private <T> TransactPutItemEnhancedRequest<T> putIfAbsent(final T item, final Class<T> itemClass) {
        return TransactPutItemEnhancedRequest.builder(itemClass)
                .item(item)
                .conditionExpression(ITEM_NOT_EXISTS)
                .build();
    }
}
