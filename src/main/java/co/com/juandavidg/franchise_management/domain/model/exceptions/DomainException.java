package co.com.juandavidg.franchise_management.domain.model.exceptions;

public abstract class DomainException extends RuntimeException {

    private final transient ErrorCode code;

    protected DomainException(final ErrorCode code, final Throwable cause) {
        super(code.getMessage(), cause);
        this.code = code;
    }

    protected DomainException(final ErrorCode code) {
        this(code, null);
    }

    public ErrorCode getCode() {
        return code;
    }
}
