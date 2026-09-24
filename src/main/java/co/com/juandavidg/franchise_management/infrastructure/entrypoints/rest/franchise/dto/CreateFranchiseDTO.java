package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.franchise.dto;

import co.com.juandavidg.franchise_management.domain.model.Franchise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to register a new franchise")
public record CreateFranchiseDTO(
        @Schema(
                description = "Unique commercial name of the franchise",
                example = "McDonald's",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name
) {

    public Franchise toDomain() {
        return Franchise.builder()
                .name(name)
                .build();
    }
}
