package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/** Spring Data CRUD repository for movement documents. */
public interface SpringDataMovementRepository extends ReactiveMongoRepository<MovementEntity, String> {
}
