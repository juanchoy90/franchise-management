package co.com.juandavidg.franchise_management.domain.ports.in;

import co.com.juandavidg.franchise_management.domain.command.DeleteProductCommand;
import reactor.core.publisher.Mono;

public interface DeleteProductUseCase {

    Mono<Void> execute(final DeleteProductCommand command);
}
