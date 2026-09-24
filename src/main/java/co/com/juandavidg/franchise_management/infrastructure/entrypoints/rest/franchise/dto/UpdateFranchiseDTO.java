package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto;

import co.com.juandavidg.franchise_management.domain.model.Franchise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to rename an existing franchise")
public record UpdateFranchiseDTO(
        @Schema(
                description = "New unique commercial name of the franchise",
                example = "Popeyes",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name
) {

    public Franchise toDomain(final String id) {
        return Franchise.builder()
                .id(id)
                .name(name)
                .build();
    }
}
