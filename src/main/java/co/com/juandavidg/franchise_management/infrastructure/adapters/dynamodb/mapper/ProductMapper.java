package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper;

import co.com.juandavidg.franchise_management.domain.model.Product;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.ProductEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.NameNormalizer;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductEntity toEntity(final Product product) {
        return ProductEntity.builder()
                .pk(ProductEntity.generatePk(product.getFranchiseId()))
                .sk(ProductEntity.generateSk(product.getBranchId(), product.getId()))
                .id(product.getId())
                .franchiseId(product.getFranchiseId())
                .branchId(product.getBranchId())
                .name(product.getName())
                .nameKey(NameNormalizer.normalize(product.getName()))
                .gsi1Pk(ProductEntity.generateGsi1Pk(product.getFranchiseId(), product.getBranchId()))
                .stock(product.getStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static Product toDomain(final ProductEntity entity) {
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
