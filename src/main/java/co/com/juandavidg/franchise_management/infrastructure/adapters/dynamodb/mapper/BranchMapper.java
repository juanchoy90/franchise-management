package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper;

import co.com.juandavidg.franchise_management.domain.model.Branch;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.BranchEntity;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper.NameNormalizer;

public final class BranchMapper {

    private BranchMapper() {
    }

    public static BranchEntity toEntity(final Branch branch) {
        return BranchEntity.builder()
                .pk(BranchEntity.generatePk(branch.getFranchiseId()))
                .sk(BranchEntity.generateSk(branch.getId()))
                .id(branch.getId())
                .franchiseId(branch.getFranchiseId())
                .name(branch.getName())
                .nameKey(NameNormalizer.normalize(branch.getName()))
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }

    public static Branch toDomain(final BranchEntity entity) {
        return Branch.builder()
                .id(entity.getId())
                .franchiseId(entity.getFranchiseId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
