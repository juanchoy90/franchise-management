package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto;

import co.com.juandavidg.franchise_management.domain.model.Franchise;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Franchise resource returned by the API")
public record FranchiseResponseDTO(
        @Schema(
                description = "Franchise unique identifier",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid")
        String id,
        @Schema(description = "Franchise commercial name", example = "McDonald's")
        String name,
        @Schema(description = "UTC instant when the franchise was created", example = "2026-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "UTC instant when the franchise was last updated", example = "2026-01-01T00:00:00Z")
        Instant updatedAt
) {

    public static FranchiseResponseDTO fromDomain(final Franchise franchise) {
        return new FranchiseResponseDTO(
                franchise.getId(),
                franchise.getName(),
                franchise.getCreatedAt(),
                franchise.getUpdatedAt());
    }
}
