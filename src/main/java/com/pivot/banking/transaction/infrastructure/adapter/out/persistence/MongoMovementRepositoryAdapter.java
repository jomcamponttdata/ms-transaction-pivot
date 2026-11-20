package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.application.port.out.MovementRepositoryPort;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/** Implements movement persistence and cursor filtering with Spring Data MongoDB. */
@Repository
public class MongoMovementRepositoryAdapter implements MovementRepositoryPort {

    private final SpringDataMovementRepository repository;
    private final ReactiveMongoTemplate template;

    /** Creates the adapter with the Spring Data repository and query template. */
    public MongoMovementRepositoryAdapter(SpringDataMovementRepository repository, ReactiveMongoTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    /** Saves a movement document. */
    @Override
    public Mono<Movement> save(Movement movement) {
        return repository.save(MovementEntity.fromDomain(movement)).map(MovementEntity::toDomain);
    }

    /** Finds a movement document by its identifier. */
    @Override
    public Mono<Movement> findById(String movementId) {
        return repository.findById(movementId).map(MovementEntity::toDomain);
    }

    /** Finds movements matching filters and the descending cursor position. */
    @Override
    public Flux<Movement> findPage(MovementFilter filter, MovementCursor cursor, int limit) {
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
            filters.add(new Criteria().orOperator(
                    Criteria.where("createdAt").lt(cursor.createdAt()),
                    new Criteria().andOperator(
                            Criteria.where("createdAt").is(cursor.createdAt()),
                            Criteria.where("_id").lt(cursor.id()))));
        }

        Criteria criteria = filters.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(filters.toArray(Criteria[]::new));
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
                .limit(limit);
        return template.find(query, MovementEntity.class).map(MovementEntity::toDomain);
    }

    /** Deletes a movement document by identifier. */
    @Override
    public Mono<Void> deleteById(String movementId) {
        return repository.deleteById(movementId);
    }
}
