package com.pivot.banking.transaction.application.service;

import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import com.pivot.banking.transaction.application.port.out.MovementRepositoryPort;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementApplicationServiceTest {

    @Mock
    private MovementRepositoryPort repository;

    private MovementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MovementApplicationService(repository);
    }

    @Test
    void createUsesTheContractCurrencyDefaultAndPersistsMovement() {
        // Arrange
        var command = new CreateMovementCommand(UUID.randomUUID(), ProductType.BANK_ACCOUNT,
                MovementType.DEPOSIT, new BigDecimal("25.50"), null, "Cash deposit");
        when(repository.save(any(Movement.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        var result = service.create(command);

        // Assert
        StepVerifier.create(result)
                .assertNext(movement -> {
                    assertNotNull(movement.id());
                    assertEquals("PEN", movement.currency());
                    assertEquals(command.productId(), movement.productId());
                    assertNotNull(movement.createdAt());
                })
                .verifyComplete();
    }

    @Test
    void findAllBuildsContinuationCursorWhenAnotherItemExists() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);
        var first = movement("first", "2025-01-03T10:00:00Z");
        var second = movement("second", "2025-01-02T10:00:00Z");
        var extra = movement("extra", "2025-01-01T10:00:00Z");
        when(repository.findPage(filter, null, 3)).thenReturn(Flux.just(first, second, extra));

        // Act
        var firstPage = service.findAll(filter, 2, null);

        // Assert
        StepVerifier.create(firstPage)
                .assertNext(page -> {
                    assertEquals(List.of(first, second), page.movements());
                    assertNotNull(page.nextCursor());
                })
                .verifyComplete();

        var cursorCaptor = ArgumentCaptor.forClass(MovementCursor.class);
        when(repository.findPage(eq(filter), cursorCaptor.capture(), eq(3)))
                .thenReturn(Flux.just(extra));
        // Act
        var secondPage = service.findAll(filter, 2, "MjAyNS0wMS0wMlQxMDowMDowMFoKc2Vjb25k");

        // Assert
        StepVerifier.create(secondPage)
                .assertNext(page -> assertNull(page.nextCursor()))
                .verifyComplete();
        assertEquals(second.id(), cursorCaptor.getValue().id());
        assertEquals(second.createdAt(), cursorCaptor.getValue().createdAt());
    }

    @Test
    void findAllRejectsInvalidCursorAndPageSizeBeforeQueryingRepository() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);

        // Act
        var oversizedPage = service.findAll(filter, 201, null);
        var invalidCursor = service.findAll(filter, 10, "bad-cursor");

        // Assert
        StepVerifier.create(oversizedPage)
                .expectError(IllegalArgumentException.class)
                .verify();
        StepVerifier.create(invalidCursor)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void findAllRejectsAnInvertedDateRange() {
        // Arrange
        var filter = new MovementFilter(null, null, Instant.parse("2025-02-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"));

        // Act
        var result = service.findAll(filter, 10, null);

        // Assert
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void updatePreservesProductAndCreationMetadata() {
        // Arrange
        var existing = movement("movement-1", "2025-01-01T00:00:00Z");
        var command = new UpdateMovementCommand(MovementType.PAYMENT, new BigDecimal("10.00"),
                "USD", "Updated");
        when(repository.findById(existing.id())).thenReturn(Mono.just(existing));
        when(repository.save(any(Movement.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        var result = service.update(existing.id(), command);

        // Assert
        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertEquals(existing.productId(), updated.productId());
                    assertEquals(existing.productType(), updated.productType());
                    assertEquals(existing.createdAt(), updated.createdAt());
                    assertEquals(MovementType.PAYMENT, updated.movementType());
                    assertEquals("USD", updated.currency());
                })
                .verifyComplete();
    }

    @Test
    void deleteReturnsNotFoundForMissingMovement() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act
        var result = service.delete("missing");

        // Assert
        StepVerifier.create(result)
                .expectError(MovementNotFoundException.class)
                .verify();

        verify(repository, never()).deleteById("missing");
    }

    private Movement movement(String id, String createdAt) {
        return new Movement(id, UUID.randomUUID(), ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("1.00"), "PEN", null, Instant.parse(createdAt));
    }
}
