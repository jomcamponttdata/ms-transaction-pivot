package com.pivot.banking.transaction.application.port.out;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/** Persistence boundary used by movement use cases. */
public interface MovementRepositoryPort {

    /** Saves a movement and returns its persisted representation. */
    Single<Movement> save(Movement movement);

    /** Finds a movement by its document identifier. */
    Maybe<Movement> findById(String movementId);

    /** Queries a bounded descending page using filters and an optional cursor. */
    Flowable<Movement> findPage(MovementFilter filter, MovementCursor cursor, int limit);

    /** Deletes a movement by identifier. */
    Completable deleteById(String movementId);
}
