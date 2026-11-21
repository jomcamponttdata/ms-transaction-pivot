package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
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
        Mono<Movement> result = adapter.save(movement);

        // Assert
        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertEquals("id-1", saved.id());
                    assertEquals(movement.productId(), saved.productId());
                    assertEquals(movement.productType(), saved.productType());
                    assertEquals(movement.movementType(), saved.movementType());
                    assertEquals(movement.amount(), saved.amount());
                    assertEquals(movement.currency(), saved.currency());
                    assertEquals(movement.description(), saved.description());
                    assertEquals(movement.createdAt(), saved.createdAt());
                })
                .verifyComplete();
        verify(repository).save(any(MovementEntity.class));
    }

    @Test
    void findByIdReturnsMovementWhenExists() {
        // Arrange
        Movement movement = movement("id-1");
        when(repository.findById("id-1")).thenReturn(Mono.just(MovementEntity.fromDomain(movement)));

        // Act
        Mono<Movement> result = adapter.findById("id-1");

        // Assert
        StepVerifier.create(result)
                .assertNext(found -> assertEquals(movement, found))
                .verifyComplete();
        verify(repository).findById("id-1");
    }

    @Test
    void findByIdReturnsEmptyWhenMovementDoesNotExist() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act
        Mono<Movement> result = adapter.findById("missing");

        // Assert
        StepVerifier.create(result).verifyComplete();
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
        Flux<Movement> result = adapter.findPage(filter, cursor, 10);

        // Assert
        StepVerifier.create(result)
                .assertNext(found -> assertEquals(movement, found))
                .verifyComplete();
        verify(template).find(any(Query.class), eq(MovementEntity.class));
    }

    @Test
    void findPageSupportsEmptyFiltersAndNoCursor() {
        // Arrange
        MovementFilter filter = new MovementFilter(null, null, null, null);
        when(template.find(any(Query.class), eq(MovementEntity.class))).thenReturn(Flux.empty());

        // Act
        Flux<Movement> result = adapter.findPage(filter, null, 20);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(template).find(any(Query.class), eq(MovementEntity.class));
    }

    @Test
    void deleteByIdDeletesMovement() {
        // Arrange
        when(repository.deleteById("id-1")).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = adapter.deleteById("id-1");

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(repository).deleteById("id-1");
    }

    private Movement movement(String id) {
        return new Movement(id, UUID.randomUUID(), ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("1.00"), "PEN", "Cash deposit", Instant.parse("2025-01-01T00:00:00Z"));
    }
}
