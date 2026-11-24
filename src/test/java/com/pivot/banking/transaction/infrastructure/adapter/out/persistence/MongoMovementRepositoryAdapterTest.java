package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoMovementRepositoryAdapterTest {

    @Mock
    private SpringDataMovementRepository repository;
    @Mock
    private ReactiveMongoTemplate template;
    @InjectMocks
    private MongoMovementRepositoryAdapter adapter;

    @Test
    void savePersistsAndReturnsMovement() {
        // Arrange
        Movement movement = movement("id-1");
        when(repository.save(any(MovementEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Single<Movement> result = adapter.save(movement);

        // Assert
        result.test()
                .assertValue(saved -> "id-1".equals(saved.id())
                        && movement.productId().equals(saved.productId())
                        && movement.productType() == saved.productType()
                        && movement.movementType() == saved.movementType()
                        && movement.amount().equals(saved.amount())
                        && movement.currency().equals(saved.currency())
                        && movement.description().equals(saved.description())
                        && movement.createdAt().equals(saved.createdAt()))
                .assertComplete();
        verify(repository).save(any(MovementEntity.class));
    }

    @Test
    void findByIdReturnsMovementWhenExists() {
        // Arrange
        Movement movement = movement("id-1");
        when(repository.findById("id-1")).thenReturn(Mono.just(MovementEntity.fromDomain(movement)));

        // Act
        Maybe<Movement> result = adapter.findById("id-1");

        // Assert
        result.test().assertValue(movement).assertComplete();
        verify(repository).findById("id-1");
    }

    @Test
    void findByIdReturnsEmptyWhenMovementDoesNotExist() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act
        Maybe<Movement> result = adapter.findById("missing");

        // Assert
        result.test().assertNoValues().assertComplete();
        verify(repository).findById("missing");
    }

    @Test
    void findPageAppliesEveryFilterAndCursor() {
        // Arrange
        Movement movement = movement("id-1");
        MovementEntity entity = MovementEntity.fromDomain(movement);
        UUID productId = movement.productId();
        MovementFilter filter = new MovementFilter(productId, ProductType.BANK_ACCOUNT,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"));
        MovementCursor cursor = new MovementCursor(Instant.parse("2025-01-02T00:00:00Z"), "cursor-id");
        when(template.find(any(Query.class), eq(MovementEntity.class))).thenReturn(Flux.just(entity));

        // Act
        Flowable<Movement> result = adapter.findPage(filter, cursor, 10);

        // Assert
        result.test().assertValue(movement).assertComplete();
        verify(template).find(any(Query.class), eq(MovementEntity.class));
    }

    @Test
    void findPageSupportsEmptyFiltersAndNoCursor() {
        // Arrange
        MovementFilter filter = new MovementFilter(null, null, null, null);
        when(template.find(any(Query.class), eq(MovementEntity.class))).thenReturn(Flux.empty());

        // Act
        Flowable<Movement> result = adapter.findPage(filter, null, 20);

        // Assert
        result.test().assertNoValues().assertComplete();
        verify(template).find(any(Query.class), eq(MovementEntity.class));
    }

    @Test
    void deleteByIdDeletesMovement() {
        // Arrange
        when(repository.deleteById("id-1")).thenReturn(Mono.empty());

        // Act
        Completable result = adapter.deleteById("id-1");

        // Assert
        result.test().assertComplete();
        verify(repository).deleteById("id-1");
    }

    private Movement movement(String id) {
        return new Movement(id, UUID.randomUUID(), ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("1.00"), "PEN", "Cash deposit", Instant.parse("2025-01-01T00:00:00Z"));
    }
}
