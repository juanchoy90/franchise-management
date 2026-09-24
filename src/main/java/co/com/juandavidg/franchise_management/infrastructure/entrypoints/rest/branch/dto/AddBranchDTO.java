package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto;

import co.com.juandavidg.franchise_management.domain.model.Branch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to add a branch to an existing franchise")
public record AddBranchDTO(
        @Schema(
                description = "Franchise that will own the branch",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String franchiseId,
        @Schema(
                description = "Unique name of the branch within the franchise",
                example = "Downtown",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name
) {

    public Branch toDomain() {
        return Branch.builder()
                .franchiseId(franchiseId)
                .name(name)
                .build();
    }
}
