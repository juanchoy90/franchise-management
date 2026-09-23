package co.com.juandavidg.franchise_management.domain.model.exceptions;

public class BusinessException extends DomainException {

    public BusinessException(final ErrorCode code) {
        super(code);
    }

    public BusinessException(final ErrorCode code, final Throwable cause) {
        super(code, cause);
    }
}
