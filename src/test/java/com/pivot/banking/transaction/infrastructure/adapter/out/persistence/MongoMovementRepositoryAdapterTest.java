package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoMovementRepositoryAdapterTest {

    @Mock
    private SpringDataMovementRepository repository;
    @Mock
    private ReactiveMongoTemplate template;

    private MongoMovementRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MongoMovementRepositoryAdapter(repository, template);
    }

    @Test
    void savePersistsAndReturnsMovement() {
        // Arrange
        var movement = movement("id-1");
        when(repository.save(any(MovementEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        var result = adapter.save(movement);

        // Assert
        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertEquals("id-1", saved.id());
                    assertEquals(movement productId(), saved productId());
                    assertEquals(movement.productType(), saved.productType());
                    assertEquals(movement.movementType(), saved.movementType());
                    assertEquals(movement.amount(), saved.amount());
                    assertEquals(movement.currency(), saved.currency());
                    assertEquals(movement.createdAt(), saved.createdAt());
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsMovementWhenExists() {
        // Arrange
        var movement = movement("id-1");
        when(repository.findById("id-1")).thenReturn(Mono.just(MovementEntity.fromDomain(movement)));

        // Act
        var result = adapter.findById("id-1");

        // Assert
        StepVerifier.create(result)
                .assertNext(found -> assertEquals("id-1", found.id()))
                .verifyComplete();
    }

    @Test
    void findByIdReturnsEmptyWhenNotExists() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act
        var result = adapter.findById("missing");

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void findPageWithFiltersAndCursor() {
        // Arrange
        var movement = movement("id-1");
        var filter = new MovementFilter(UUID.randomUUID(), ProductType.BANK_ACCOUNT,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"));
        var cursor = new MovementCursor(Instant.parse("2025-01-01T00:00:00Z"), "cursor-id");
        when(template.find(any(Query.class), any(Class.class))).thenReturn(Flux.just(MovementEntity.fromDomain(movement)));

        // Act
        var result = adapter.findPage(filter, cursor, 10);

        // Assert
        StepVerifier.create(result)
                .assertNext(found -> assertEquals("id-1", found.id()))
                .verifyComplete();
        verify(template).find(any(Query.class), any(Class.class));
    }

    @Test
    void deleteByIdDeletesMovement() {
        // Arrange
        when(repository.findById("id-1")).thenReturn(Mono.just(MovementEntity.fromDomain(movement("id-1"))));
        when(repository.delete(any(MovementEntity.class))).thenReturn(Mono.empty());

        // Act
        var result = adapter.findById("id-1").flatMap(m -> adapter.deleteById("id-1"));

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void deleteByIdReturnsNotFoundWhenMovementDoesNotExist() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act
        var result = adapter.findById("missing").flatMap(m -> adapter.deleteById("missing"));

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    private Movement movement(String id) {
        return new Movement(id, UUID.randomUUID(), ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("1.00"), "PEN", null, Instant.now());
    }
}