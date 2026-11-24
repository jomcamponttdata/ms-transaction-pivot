package com.pivot.banking.transaction.application.port.in;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementPage;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.util.UUID;

/** Inbound contracts for creating, querying, updating, and deleting movements. */
public final class MovementUseCases {

    private MovementUseCases() {
    }

    /** Input fields used to register a movement. */
    public record CreateMovementCommand(
            UUID productId,
            ProductType productType,
            MovementType movementType,
            BigDecimal amount,
            String currency,
            String description) {
    }

    /** Editable movement fields accepted by the update operation. */
    public record UpdateMovementCommand(
            MovementType movementType,
            BigDecimal amount,
            String currency,
            String description) {
    }

    /** Registers a movement. */
    public interface CreateMovement {
        /** Creates and persists a movement. */
        Single<Movement> create(CreateMovementCommand command);
    }

    /** Queries movement history. */
    public interface GetMovements {
        /** Returns a bounded page and optional continuation cursor. */
        Single<MovementPage> findAll(MovementFilter filter, int limit, String cursor);

        /** Finds a movement or reports that it does not exist. */
        Single<Movement> findById(String movementId);
    }

    /** Updates editable movement fields. */
    public interface UpdateMovement {
        /** Replaces the editable fields of an existing movement. */
        Single<Movement> update(String movementId, UpdateMovementCommand command);
    }

    /** Deletes movements. */
    public interface DeleteMovement {
        /** Deletes an existing movement. */
        Completable delete(String movementId);
    }
}
