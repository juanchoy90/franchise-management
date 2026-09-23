package co.com.juandavidg.franchise_management.domain.model.exceptions;

public class TechnicalException extends DomainException {

    public TechnicalException(final ErrorCode code, final Throwable cause) {
        super(code, cause);
    }

    public TechnicalException(final ErrorCode code) {
        super(code);
    }
}
