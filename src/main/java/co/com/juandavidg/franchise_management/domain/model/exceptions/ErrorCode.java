package co.com.juandavidg.franchise_management.domain.model.exceptions;

public enum ErrorCode {

    FRANCHISE_NOT_FOUND("The requested franchise does not exist"),
    FRANCHISE_ALREADY_EXISTS("A franchise with that name already exists"),
    BRANCH_NOT_FOUND("The requested branch does not exist"),
    BRANCH_ALREADY_EXISTS("A branch with that name already exists in the franchise"),
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
