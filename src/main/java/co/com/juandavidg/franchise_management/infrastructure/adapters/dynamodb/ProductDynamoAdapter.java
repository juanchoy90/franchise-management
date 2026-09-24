package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.domain.ports.out.ProductRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.DynamoResilienceDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
@Repository
public class ProductDynamoAdapter implements ProductRepositoryPort {

    private static final Expression ITEM_NOT_EXISTS = Expression.builder()
            .expression("attribute_not_exists(PK) AND attribute_not_exists(SK)")
            .build();

    private final DynamoDbAsyncTable<ProductEntity> table;
    private final DynamoResilienceDecorator decorator;

    public ProductDynamoAdapter(
            final DynamoDbEnhancedAsyncClient enhancedClient,
            final DynamoDbProperties properties,
            final DynamoResilienceDecorator decorator) {
        this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(ProductEntity.class));
        this.decorator = decorator;
    }

    @Override
    public Mono<Product> save(final Product product) {
        log.debug("Saving product {} in branch {}", product.getName(), product.getBranchId());
        final ProductEntity entity = toEntity(product);
        final PutItemEnhancedRequest<ProductEntity> request = PutItemEnhancedRequest.builder(ProductEntity.class)
                .item(entity)
                .conditionExpression(ITEM_NOT_EXISTS)
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> table.putItem(request))
                        .thenReturn(toDomain(entity)),
                "saveProduct");
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndBranchIdAndName(
            final String franchiseId,
            final String branchId,
            final String name) {
        log.debug("Checking product {} in branch {} of franchise {}", name, branchId, franchiseId);
        final QueryConditional conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(ProductEntity.generatePk(franchiseId))
                .sortValue(ProductEntity.generateSk(branchId, ""))
                .build());

        return decorator.decorate(
                Flux.from(table.query(request -> request.queryConditional(conditional)))
                        .flatMapIterable(Page::items)
                        .filter(item -> item.getName() != null && name.equalsIgnoreCase(item.getName()))
                        .hasElements(),
                "existsProduct");
    }

    private static ProductEntity toEntity(final Product product) {
        return ProductEntity.builder()
                .pk(ProductEntity.generatePk(product.getFranchiseId()))
                .sk(ProductEntity.generateSk(product.getBranchId(), product.getId()))
                .id(product.getId())
                .franchiseId(product.getFranchiseId())
                .branchId(product.getBranchId())
                .name(product.getName())
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
