package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto;

import co.com.juandavidg.franchise_management.domain.model.Branch;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Branch resource returned by the API")
public record BranchResponseDTO(
        @Schema(
                description = "Branch unique identifier",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                format = "uuid")
        String id,
        @Schema(
                description = "Franchise that owns the branch",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid")
        String franchiseId,
        @Schema(description = "Branch commercial name", example = "Downtown")
        String name,
        @Schema(description = "UTC instant when the branch was created", example = "2026-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "UTC instant when the branch was last updated", example = "2026-01-01T00:00:00Z")
        Instant updatedAt
) {

    public static BranchResponseDTO fromDomain(final Branch branch) {
        return new BranchResponseDTO(
                branch.getId(),
                branch.getFranchiseId(),
                branch.getName(),
                branch.getCreatedAt(),
                branch.getUpdatedAt());
    }
}
