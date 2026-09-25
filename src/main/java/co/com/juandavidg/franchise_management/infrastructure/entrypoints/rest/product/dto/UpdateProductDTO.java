package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto;

import co.com.juandavidg.franchise_management.domain.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to rename an existing product")
public record UpdateProductDTO(
        @Schema(
                description = "New unique name of the product within the branch",
                example = "Burger",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most {max} characters")
        String name
) {

    public Product toDomain(final String franchiseId, final String branchId, final String id) {
        return Product.builder()
                .id(id)
                .franchiseId(franchiseId)
                .branchId(branchId)
                .name(name)
                .build();
    }
}
