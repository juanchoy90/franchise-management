package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@Schema(description = "Stable error payload returned when a request cannot be completed")
public class ErrorResponse {

    @Schema(description = "Machine-readable error code", example = "FRANCHISE_NOT_FOUND")
    String code;
    @Schema(description = "Client-safe message taken from the domain error code", example = "The requested franchise does not exist")
    String message;
    @Schema(description = "Request path that produced the error", example = "/v1/franchises")
    String path;
    @Schema(description = "UTC instant when the error was produced", example = "2026-01-01T00:00:00Z")
    Instant timestamp;
    @Schema(description = "Correlation identifier propagated in X-Trace-Id", example = "8f3c1d2e-4b5a-6789-abcd-ef0123456789")
    String traceId;
}
