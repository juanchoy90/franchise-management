package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.branch.dto;

import co.com.juandavidg.franchise_management.domain.model.Branch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to rename an existing branch")
public record UpdateBranchDTO(
        @Schema(
                description = "New unique name of the branch within the franchise",
                example = "Airport",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name
) {

    public Branch toDomain(final String franchiseId, final String id) {
        return Branch.builder()
                .id(id)
                .franchiseId(franchiseId)
                .name(name)
                .build();
    }
}
