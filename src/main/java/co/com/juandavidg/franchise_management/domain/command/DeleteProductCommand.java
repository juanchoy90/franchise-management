package co.com.juandavidg.franchise_management.domain.command;

public record DeleteProductCommand(String franchiseId, String branchId, String productId) {
}
