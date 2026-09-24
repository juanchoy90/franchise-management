package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.openapi;

final class OpenApiExamples {

    static final String CREATE_FRANCHISE = """
            {"name":"McDonald's"}
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

    private OpenApiExamples() {
    }
}
