package com.pivot.banking.transaction.application.service;

import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.DeleteMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.GenerateProductReport;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.GetMovements;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import com.pivot.banking.transaction.application.port.out.MovementRepositoryPort;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementPage;
import com.pivot.banking.transaction.domain.MovementSummaryCalculator;
import com.pivot.banking.transaction.domain.ProductMovementReport;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Coordinates movement CRUD, cursor-paginated history queries, and product reports. */
@Service
public class MovementApplicationService
    implements CreateMovement, GetMovements, GenerateProductReport, UpdateMovement, DeleteMovement {

  /** Largest page accepted by the movement history query; bounds each report snapshot. */
  private static final int REPORT_MOVEMENT_LIMIT = 200;

  private final MovementRepositoryPort repository;

  /** Creates the application service with its persistence port. */
  public MovementApplicationService(MovementRepositoryPort repository) {
    this.repository = repository;
  }

  /** Creates a movement using the default currency when the caller omits it. */
  @Override
  public Single<Movement> create(CreateMovementCommand command) {
    var movement =
        new Movement(
            UUID.randomUUID().toString(),
            command.productId(),
            command.productType(),
            command.movementType(),
            command.amount(),
            command.currency() == null ? "PEN" : command.currency(),
            command.description(),
            Instant.now());
    return repository.save(movement);
  }

  /** Queries movement history and creates a cursor only when another page exists. */
  @Override
  public Single<MovementPage> findAll(MovementFilter filter, int limit, String cursorToken) {
    if (limit < 1 || limit > 200) {
      return Single.error(new IllegalArgumentException("limit must be between 1 and 200"));
    }
    var rangeError = invalidDateRange(filter);
    if (rangeError != null) {
      return Single.error(rangeError);
    }

    MovementCursor cursor;
    try {
      cursor = decodeCursor(cursorToken);
    } catch (IllegalArgumentException exception) {
      return Single.error(exception);
    }
    return repository
        .findPage(filter, cursor, limit + 1)
        .toList()
        .map(
            found -> {
              boolean hasNext = found.size() > limit;
              var pageMovements = hasNext ? found.subList(0, limit) : found;
              String nextCursor =
                  hasNext ? encodeCursor(pageMovements.get(pageMovements.size() - 1)) : null;
              return new MovementPage(pageMovements, nextCursor);
            });
  }

  /**
   * Builds a report from a single bounded read of the existing movement history query. Only the
   * first movements matching the filter are considered; the bound is documented in the API
   * contract.
   */
  @Override
  public Single<ProductMovementReport> generate(MovementFilter filter) {
    var rangeError = invalidDateRange(filter);
    if (rangeError != null) {
      return Single.error(rangeError);
    }
    return repository
        .findPage(filter, null, REPORT_MOVEMENT_LIMIT)
        .toList()
        .map(
            movements ->
                new ProductMovementReport(
                    movements, MovementSummaryCalculator.summarize(movements)));
  }

  /** Retrieves a movement or raises a not-found error. */
  @Override
  public Single<Movement> findById(String movementId) {
    return repository
        .findById(movementId)
        .switchIfEmpty(Single.error(new MovementNotFoundException(movementId)));
  }

  /** Updates an existing movement while retaining its product identity and creation time. */
  @Override
  public Single<Movement> update(String movementId, UpdateMovementCommand command) {
    return findById(movementId)
        .map(
            movement ->
                movement.update(
                    command.movementType(),
                    command.amount(),
                    command.currency(),
                    command.description()))
        .flatMap(repository::save);
  }

  /** Deletes an existing movement. */
  @Override
  public Completable delete(String movementId) {
    return findById(movementId).flatMapCompletable(movement -> repository.deleteById(movementId));
  }

  /**
   * Returns the shared range validation error, or null when the interval is valid or only partly
   * bounded.
   */
  private IllegalArgumentException invalidDateRange(MovementFilter filter) {
    if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
      return new IllegalArgumentException("'from' must not be after 'to'");
    }
    return null;
  }

  /** Encodes the last returned movement position as an opaque URL-safe token. */
  private String encodeCursor(Movement movement) {
    String value = movement.createdAt() + "\n" + movement.id();
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  /** Decodes a cursor token, rejecting malformed timestamp/id pairs. */
  private MovementCursor decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      int separator = decoded.indexOf('\n');
      if (separator <= 0 || separator == decoded.length() - 1) {
        throw new IllegalArgumentException("Invalid movement cursor");
      }
      return new MovementCursor(
          Instant.parse(decoded.substring(0, separator)), decoded.substring(separator + 1));
    } catch (IllegalArgumentException | DateTimeException exception) {
      throw new IllegalArgumentException("Invalid movement cursor", exception);
    }
  }
}
