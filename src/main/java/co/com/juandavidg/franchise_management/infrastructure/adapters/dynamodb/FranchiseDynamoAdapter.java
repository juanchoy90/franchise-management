package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.domain.ports.out.FranchiseRepositoryPort;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.config.DynamoDbProperties;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.FranchiseEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.FranchiseNameLockEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.DynamoResilienceDecorator;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper.FranchiseMapper;
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

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Repository
public class FranchiseDynamoAdapter implements FranchiseRepositoryPort {

    private static final Expression ITEM_NOT_EXISTS = Expression.builder()
            .expression("attribute_not_exists(PK) AND attribute_not_exists(SK)")
            .build();

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncTable<FranchiseEntity> table;
    private final DynamoDbAsyncTable<FranchiseNameLockEntity> nameLockTable;
    private final DynamoResilienceDecorator decorator;

    public FranchiseDynamoAdapter(
            final DynamoDbEnhancedAsyncClient enhancedClient,
            final DynamoDbProperties properties,
            final DynamoResilienceDecorator decorator) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(FranchiseEntity.class));
        this.nameLockTable = enhancedClient.table(
                properties.tableName(),
                TableSchema.fromBean(FranchiseNameLockEntity.class));
        this.decorator = decorator;
    }

    @Override
    public Mono<Franchise> save(final Franchise franchise) {
        log.debug("Saving franchise: {}", franchise.getName());
        final FranchiseEntity entity = FranchiseMapper.toEntity(franchise);
        final FranchiseNameLockEntity nameLock = FranchiseNameLockEntity.from(franchise.getId(), franchise.getName());
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, putIfAbsent(entity, FranchiseEntity.class))
                .addPutItem(nameLockTable, putIfAbsent(nameLock, FranchiseNameLockEntity.class))
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(FranchiseMapper.toDomain(entity)),
                "saveFranchise");
    }

    @Override
    public Mono<Franchise> findById(final String id) {
        log.debug("Finding franchise by id: {}", id);
        final Key key = Key.builder()
                .partitionValue(FranchiseEntity.generatePk(id))
                .sortValue(FranchiseEntity.generateSk())
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> table.getItem(key).thenApply(Optional::ofNullable))
                        .flatMap(Mono::justOrEmpty)
                        .map(FranchiseMapper::toDomain),
                "findFranchiseById");
    }

    @Override
    public Mono<Franchise> updateName(final String id, final String newName) {
        log.debug("Updating franchise name: {} -> {}", id, newName);
        return findById(id)
                .flatMap(existing -> persistName(existing, newName));
    }

    @Override
    public Mono<Boolean> existsByName(final String name) {
        log.debug("Checking if franchise exists by name: {}", name);
        final Key key = Key.builder()
                .partitionValue(FranchiseNameLockEntity.generatePk(name))
                .sortValue(FranchiseNameLockEntity.generateSk())
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> nameLockTable.getItem(key)).hasElement(),
                "existsFranchiseByName");
    }

    private Mono<Franchise> persistName(final Franchise existing, final String newName) {
        final Franchise updated = existing.toBuilder()
                .name(newName)
                .updatedAt(Instant.now())
                .build();
        return Mono.just(updated)
                .filter(franchise -> !FranchiseNameLockEntity.generatePk(existing.getName())
                        .equals(FranchiseNameLockEntity.generatePk(newName)))
                .flatMap(franchise -> persistRenamedFranchise(existing.getName(), franchise))
                .switchIfEmpty(overwriteMetadata(updated));
    }

    private Mono<Franchise> persistRenamedFranchise(final String existingName, final Franchise updated) {
        final FranchiseEntity entity = FranchiseMapper.toEntity(updated);
        final TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(table, entity)
                .addPutItem(nameLockTable, putIfAbsent(
                        FranchiseNameLockEntity.from(updated.getId(), updated.getName()),
                        FranchiseNameLockEntity.class))
                .addDeleteItem(nameLockTable, Key.builder()
                        .partitionValue(FranchiseNameLockEntity.generatePk(existingName))
                        .sortValue(FranchiseNameLockEntity.generateSk())
                        .build())
                .build();

        return decorator.decorate(
                Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
                        .thenReturn(FranchiseMapper.toDomain(entity)),
                "updateFranchiseName");
    }

    private Mono<Franchise> overwriteMetadata(final Franchise franchise) {
        final FranchiseEntity entity = FranchiseMapper.toEntity(franchise);
        return decorator.decorate(
                Mono.fromFuture(() -> table.putItem(entity))
                        .thenReturn(FranchiseMapper.toDomain(entity)),
                "updateFranchiseName");
    }

    private <T> TransactPutItemEnhancedRequest<T> putIfAbsent(final T item, final Class<T> itemClass) {
        return TransactPutItemEnhancedRequest.builder(itemClass)
                .item(item)
                .conditionExpression(ITEM_NOT_EXISTS)
                .build();
    }
}
