package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.application.port.out.MovementRepositoryPort;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Implements movement persistence and cursor filtering with Spring Data MongoDB. */
@Repository
public class MongoMovementRepositoryAdapter implements MovementRepositoryPort {

  private final SpringDataMovementRepository repository;
  private final ReactiveMongoTemplate template;

  /** Creates the adapter with the Spring Data repository and query template. */
  public MongoMovementRepositoryAdapter(
      SpringDataMovementRepository repository, ReactiveMongoTemplate template) {
    this.repository = repository;
    this.template = template;
  }

  /** Saves a movement document. */
  @Override
  public Single<Movement> save(Movement movement) {
    Mono<Movement> result =
        repository.save(MovementEntity.fromDomain(movement)).map(MovementEntity::toDomain);
    return RxJava3Adapter.monoToSingle(result);
  }

  /** Finds a movement document by its identifier. */
  @Override
  public Maybe<Movement> findById(String movementId) {
    Mono<Movement> result = repository.findById(movementId).map(MovementEntity::toDomain);
    return RxJava3Adapter.monoToMaybe(result);
  }

  /** Finds movements matching filters and the descending cursor position. */
  @Override
  public Flowable<Movement> findPage(MovementFilter filter, MovementCursor cursor, int limit) {
    List<Criteria> filters = new ArrayList<>();
    if (filter.productId() != null) {
      filters.add(Criteria.where("productId").is(filter.productId()));
    }
    if (filter.productType() != null) {
      filters.add(Criteria.where("productType").is(filter.productType().name()));
    }
    if (filter.from() != null) {
      filters.add(Criteria.where("createdAt").gte(filter.from()));
    }
    if (filter.to() != null) {
      filters.add(Criteria.where("createdAt").lte(filter.to()));
    }
    if (cursor != null) {
      filters.add(
          new Criteria()
              .orOperator(
                  Criteria.where("createdAt").lt(cursor.createdAt()),
                  new Criteria()
                      .andOperator(
                          Criteria.where("createdAt").is(cursor.createdAt()),
                          Criteria.where("_id").lt(cursor.id()))));
    }

    Criteria criteria =
        filters.isEmpty()
            ? new Criteria()
            : new Criteria().andOperator(filters.toArray(Criteria[]::new));
    Query query =
        Query.query(criteria)
            .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
            .limit(limit);
    Flux<Movement> result =
        template.find(query, MovementEntity.class).map(MovementEntity::toDomain);
    return RxJava3Adapter.fluxToFlowable(result);
  }

  /** Deletes a movement document by identifier. */
  @Override
  public Completable deleteById(String movementId) {
    Mono<Void> result = repository.deleteById(movementId);
    return RxJava3Adapter.monoToCompletable(result);
  }
}
