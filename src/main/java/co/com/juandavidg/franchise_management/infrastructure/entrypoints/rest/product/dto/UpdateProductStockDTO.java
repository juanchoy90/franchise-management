package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.product.dto;

import co.com.juandavidg.franchise_management.domain.command.UpdateProductStockCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Signed stock adjustment applied atomically to an existing product")
public record UpdateProductStockDTO(
        @Schema(
                description = "Stock delta. Positive increases stock, negative decreases it.",
                example = "5",
                minimum = "-10000000",
                maximum = "10000000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "must not be null")
        @Min(value = UpdateProductStockCommand.MIN_DELTA, message = "must be greater than or equal to {value}")
        @Max(value = UpdateProductStockCommand.MAX_DELTA, message = "must be less than or equal to {value}")
        Integer delta
) {

    public UpdateProductStockCommand toCommand(
            final String franchiseId,
            final String branchId,
            final String productId) {
        return new UpdateProductStockCommand(franchiseId, branchId, productId, delta);
    }
}
