package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductNameLockEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.DynamoResilienceDecorator;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.NameNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

@Slf4j
@Repository
public class ProductDynamoAdapter implements ProductRepositoryPort {

    private static final Expression ITEM_NOT_EXISTS = Expression.builder()
            .expression("attribute_not_exists(PK) AND attribute_not_exists(SK)")
            .build();

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncTable<ProductEntity> table;
    private final DynamoDbAsyncTable<ProductNameLockEntity> nameLockTable;
    private final DynamoResilienceDecorator decorator;

    public ProductDynamoAdapter(
            final DynamoDbEnhancedAsyncClient enhancedClient,
            final DynamoDbProperties properties,
            final DynamoResilienceDecorator decorator) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(ProductEntity.class));
        this.nameLockTable = enhancedClient.table(
                properties.tableName(),
                TableSchema.fromBean(ProductNameLockEntity.class));
        this.decorator = decorator;
    }

    @Override
    public Mono<Product> save(final Product product) {
        log.debug("Saving product {} in branch {}", product.getName(), product.getBranchId());
        final ProductEntity entity = toEntity(product);
        final ProductNameLockEntity nameLock = ProductNameLockEntity.from(
                product.getFranchiseId(), product.getBranchId(), product.getId(), product.getName());
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, putIfAbsent(entity, ProductEntity.class))
                .addPutItem(nameLockTable, putIfAbsent(nameLock, ProductNameLockEntity.class))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(toDomain(entity)),
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

    private <T> TransactPutItemEnhancedRequest<T> putIfAbsent(final T item, final Class<T> itemClass) {
        return TransactPutItemEnhancedRequest.builder(itemClass)
                .item(item)
                .conditionExpression(ITEM_NOT_EXISTS)
                .build();
    }

    private static ProductEntity toEntity(final Product product) {
        return ProductEntity.builder()
                .pk(ProductEntity.generatePk(product.getFranchiseId()))
                .sk(ProductEntity.generateSk(product.getBranchId(), product.getId()))
                .id(product.getId())
                .franchiseId(product.getFranchiseId())
                .branchId(product.getBranchId())
                .name(product.getName())
                .nameKey(NameNormalizer.normalize(product.getName()))
                .gsi1Pk(ProductEntity.generateGsi1Pk(product.getFranchiseId(), product.getBranchId()))
                .gsi1Sk(product.getStock())
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private static Product toDomain(final ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .franchiseId(entity.getFranchiseId())
                .branchId(entity.getBranchId())
                .name(entity.getName())
                .stock(entity.getStock())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
