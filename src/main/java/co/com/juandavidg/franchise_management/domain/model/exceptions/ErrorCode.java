package co.com.juandavidg.franchise_management.domain.model.exceptions;

public enum ErrorCode {

    FRANCHISE_NOT_FOUND("The requested franchise does not exist"),
    FRANCHISE_ALREADY_EXISTS("A franchise with that name already exists"),
    BRANCH_NOT_FOUND("The requested branch does not exist"),
    BRANCH_ALREADY_EXISTS("A branch with that name already exists in the franchise"),
    PRODUCT_ALREADY_EXISTS("A product with that name already exists in the branch"),
    PRODUCT_NOT_FOUND("The requested product does not exist"),
    INVALID_STOCK("Stock cannot be negative"),
    INSUFFICIENT_STOCK("The product does not have enough stock"),
    STOCK_DELTA_OUT_OF_RANGE("Stock delta must be between -10000000 and 10000000"),
    VALIDATION_ERROR("The request is syntactically invalid"),

    PERSISTENCE_ERROR("Error processing the request"),
    SERVICE_UNAVAILABLE("The service is temporarily unavailable"),
    SERVICE_THROTTLED("Too many requests, please try again later"),
    TIMEOUT("The operation took too long to respond"),
    UNEXPECTED_ERROR("An unexpected error occurred");

    private final String message;

    ErrorCode(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
