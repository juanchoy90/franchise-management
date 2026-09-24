package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto;

import co.com.juandavidg.franchise_management.domain.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Product resource returned by the API")
public record ProductResponseDTO(
        @Schema(
                description = "Product unique identifier",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                format = "uuid")
        String id,
        @Schema(
                description = "Franchise that owns the product",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid")
        String franchiseId,
        @Schema(
                description = "Branch that owns the product",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                format = "uuid")
        String branchId,
        @Schema(description = "Product commercial name", example = "Fries")
        String name,
        @Schema(description = "Current stock of the product", example = "10")
        Integer stock,
        @Schema(description = "UTC instant when the product was created", example = "2026-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "UTC instant when the product was last updated", example = "2026-01-01T00:00:00Z")
        Instant updatedAt
) {

    public static ProductResponseDTO fromDomain(final Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getFranchiseId(),
                product.getBranchId(),
                product.getName(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
