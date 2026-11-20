package com.pivot.banking.transaction.application.port.out;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence boundary used by movement use cases. */
public interface MovementRepositoryPort {

    /** Saves a movement and returns its persisted representation. */
    Mono<Movement> save(Movement movement);

    /** Finds a movement by its document identifier. */
    Mono<Movement> findById(String movementId);

    /** Queries a bounded descending page using filters and an optional cursor. */
    Flux<Movement> findPage(MovementFilter filter, MovementCursor cursor, int limit);

    /** Deletes a movement by identifier. */
    Mono<Void> deleteById(String movementId);
}
