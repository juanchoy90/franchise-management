package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.domain.ports.out.BranchRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.BranchEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.BranchNameLockEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.DynamoResilienceDecorator;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper.BranchMapper;
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
public class BranchDynamoAdapter implements BranchRepositoryPort {

    private static final Expression ITEM_NOT_EXISTS = Expression.builder()
            .expression("attribute_not_exists(PK) AND attribute_not_exists(SK)")
            .build();

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncTable<BranchEntity> table;
    private final DynamoDbAsyncTable<BranchNameLockEntity> nameLockTable;
    private final DynamoResilienceDecorator decorator;

    public BranchDynamoAdapter(
            final DynamoDbEnhancedAsyncClient enhancedClient,
            final DynamoDbProperties properties,
            final DynamoResilienceDecorator decorator) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(BranchEntity.class));
        this.nameLockTable = enhancedClient.table(
                properties.tableName(),
                TableSchema.fromBean(BranchNameLockEntity.class));
        this.decorator = decorator;
    }

    @Override
    public Mono<Branch> save(final Branch branch) {
        log.debug("Saving branch {} in franchise {}", branch.getName(), branch.getFranchiseId());
        final BranchEntity entity = BranchMapper.toEntity(branch);
        final BranchNameLockEntity nameLock = BranchNameLockEntity.from(
                branch.getFranchiseId(), branch.getId(), branch.getName());
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, putIfAbsent(entity, BranchEntity.class))
                .addPutItem(nameLockTable, putIfAbsent(nameLock, BranchNameLockEntity.class))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(BranchMapper.toDomain(entity)),
                "saveBranch");
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndName(final String franchiseId, final String name) {
        log.debug("Checking branch {} in franchise {}", name, franchiseId);
        final Key key = Key.builder()
                .partitionValue(BranchNameLockEntity.generatePk(franchiseId))
                .sortValue(BranchNameLockEntity.generateSk(name))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> nameLockTable.getItem(key)).hasElement(),
                "existsBranch");
    }

    private <T> TransactPutItemEnhancedRequest<T> putIfAbsent(final T item, final Class<T> itemClass) {
        return TransactPutItemEnhancedRequest.builder(itemClass)
                .item(item)
                .conditionExpression(ITEM_NOT_EXISTS)
                .build();
    }
}
