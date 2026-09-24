package co.com.juandavidg.franchise_management.infrastructure.entrypoints.rest.exception;

import co.com.juandavidg.franchise_management.domain.model.exceptions.ErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpStatusResolver {

    private static final Map<ErrorCode, HttpStatus> HTTP_STATUS = Map.ofEntries(
            Map.entry(ErrorCode.FRANCHISE_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.FRANCHISE_ALREADY_EXISTS, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.BRANCH_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ErrorCode.BRANCH_ALREADY_EXISTS, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.PRODUCT_ALREADY_EXISTS, HttpStatus.CONFLICT),
            Map.entry(ErrorCode.INVALID_STOCK, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST),
            Map.entry(ErrorCode.PERSISTENCE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry(ErrorCode.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ErrorCode.SERVICE_THROTTLED, HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(ErrorCode.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT),
            Map.entry(ErrorCode.UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
    );

    public HttpStatus resolve(final ErrorCode code) {
        return HTTP_STATUS.getOrDefault(code, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
