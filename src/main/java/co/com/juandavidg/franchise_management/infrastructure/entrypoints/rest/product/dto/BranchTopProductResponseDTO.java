package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto;

import co.com.juandavidg.franchise_management.domain.model.BranchTopProduct;
import co.com.juandavidg.franchise_management.domain.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@Schema(description = "Branch together with the product that currently holds the highest stock")
public record BranchTopProductResponseDTO(
        @Schema(
                description = "Branch unique identifier",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                format = "uuid")
        String branchId,
        @Schema(description = "Branch commercial name", example = "Downtown")
        String branchName,
        @Schema(description = "Highest-stock product in the branch. Null when the branch has no products.")
        TopProduct product
) {

    @Schema(description = "Product ranked by stock for a branch")
    public record TopProduct(
            @Schema(
                    description = "Product unique identifier",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    format = "uuid")
            String id,
            @Schema(description = "Product commercial name", example = "Fries")
            String name,
            @Schema(description = "Current stock of the product", example = "40")
            Integer stock
    ) {

        private static TopProduct fromDomain(final Product product) {
            return new TopProduct(product.getId(), product.getName(), product.getStock());
        }
    }

    public static BranchTopProductResponseDTO fromDomain(final BranchTopProduct item) {
        return new BranchTopProductResponseDTO(
                item.branchId(),
                item.branchName(),
                Optional.ofNullable(item.product()).map(TopProduct::fromDomain).orElse(null));
    }
}
