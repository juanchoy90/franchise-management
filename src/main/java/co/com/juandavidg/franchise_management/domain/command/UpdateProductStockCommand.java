package co.com.juandavidg.franchise_management.domain.command;

public record UpdateProductStockCommand(
        String franchiseId,
        String branchId,
        String productId,
        Integer delta) {

    public static final int MIN_DELTA = -10_000_000;
    public static final int MAX_DELTA = 10_000_000;
}
