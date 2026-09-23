package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.mapper;

import co.com.juandavidg.franchise_management.domain.model.Franchise;
import co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.entity.FranchiseEntity;

public final class FranchiseMapper {

    private FranchiseMapper() {
    }

    public static FranchiseEntity toEntity(final Franchise franchise) {
        return FranchiseEntity.builder()
                .pk(FranchiseEntity.generatePk(franchise.getId()))
                .sk(FranchiseEntity.generateSk())
                .id(franchise.getId())
                .name(franchise.getName())
                .createdAt(franchise.getCreatedAt())
                .updatedAt(franchise.getUpdatedAt())
                .build();
    }

    public static Franchise toDomain(final FranchiseEntity entity) {
        return Franchise.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
