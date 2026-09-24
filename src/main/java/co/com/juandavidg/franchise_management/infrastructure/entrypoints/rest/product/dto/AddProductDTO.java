package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto;

import co.com.juandavidg.franchise_management.domain.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to add a product to an existing branch")
public record AddProductDTO(
        @Schema(
                description = "Franchise that owns the branch",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String franchiseId,
        @Schema(
                description = "Branch that will own the product",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String branchId,
        @Schema(
                description = "Unique name of the product within the branch",
                example = "Fries",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name,
        @Schema(
                description = "Initial stock of the product",
                example = "10",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer stock
) {

    public Product toDomain() {
        return Product.builder()
                .franchiseId(franchiseId)
                .branchId(branchId)
                .name(name)
                .stock(stock)
                .build();
    }
}
