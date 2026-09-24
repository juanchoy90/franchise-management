package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

final class OpenApiExamples {

    static final String CREATE_FRANCHISE = """
            {"name":"McDonald's"}
            """;

    static final String UPDATE_FRANCHISE = """
            {"name":"Popeyes"}
            """;

    static final String FRANCHISE = """
            {"id":"550e8400-e29b-41d4-a716-446655440000","name":"McDonald's","createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
            """;

    static final String VALIDATION_ERROR = """
            {"code":"VALIDATION_ERROR","message":"name: must not be blank","path":"/v1/franchises","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String DUPLICATE_FRANCHISE = """
            {"code":"FRANCHISE_ALREADY_EXISTS","message":"A franchise with that name already exists","path":"/v1/franchises","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String FRANCHISE_NOT_FOUND = """
            {"code":"FRANCHISE_NOT_FOUND","message":"The requested franchise does not exist","path":"/v1/franchises/550e8400-e29b-41d4-a716-446655440000","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String UNEXPECTED_ERROR = """
            {"code":"UNEXPECTED_ERROR","message":"An unexpected error occurred","path":"/v1/franchises","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String CREATE_BRANCH = """
            {"franchiseId":"550e8400-e29b-41d4-a716-446655440000","name":"Downtown"}
            """;

    static final String BRANCH = """
            {"id":"7c9e6679-7425-40de-944b-e07fc1f90ae7","franchiseId":"550e8400-e29b-41d4-a716-446655440000","name":"Downtown","createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
            """;

    static final String DUPLICATE_BRANCH = """
            {"code":"BRANCH_ALREADY_EXISTS","message":"A branch with that name already exists in the franchise","path":"/v1/branches","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String CREATE_PRODUCT = """
            {"franchiseId":"550e8400-e29b-41d4-a716-446655440000","branchId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","name":"Fries","stock":10}
            """;

    static final String PRODUCT = """
            {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","franchiseId":"550e8400-e29b-41d4-a716-446655440000","branchId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","name":"Fries","stock":10,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
            """;

    static final String DUPLICATE_PRODUCT = """
            {"code":"PRODUCT_ALREADY_EXISTS","message":"A product with that name already exists in the branch","path":"/v1/products","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String UPDATE_PRODUCT_STOCK = """
            {"delta":15}
            """;

    static final String PRODUCT_STOCK_UPDATED = """
            {"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","franchiseId":"550e8400-e29b-41d4-a716-446655440000","branchId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","name":"Fries","stock":25,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}
            """;

    static final String PRODUCT_NOT_FOUND = """
            {"code":"PRODUCT_NOT_FOUND","message":"The requested product does not exist","path":"/v1/products/3fa85f64-5717-4562-b3fc-2c963f66afa6","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String INSUFFICIENT_STOCK = """
            {"code":"INSUFFICIENT_STOCK","message":"The product does not have enough stock","path":"/v1/products/3fa85f64-5717-4562-b3fc-2c963f66afa6","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String BRANCH_NOT_FOUND = """
            {"code":"BRANCH_NOT_FOUND","message":"The requested branch does not exist","path":"/v1/products","timestamp":"2026-01-01T00:00:00Z","traceId":"8f3c1d2e-4b5a-6789-abcd-ef0123456789"}
            """;

    static final String TOP_STOCK_PRODUCTS = """
            [{"branchId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","branchName":"Downtown","product":{"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"Fries","stock":40}},{"branchId":"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d","branchName":"Airport","product":null}]
            """;

    private OpenApiExamples() {
    }
}
