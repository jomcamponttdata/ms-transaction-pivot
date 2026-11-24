package com.pivot.banking.transaction.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for movement invariants and immutable updates. */
class MovementTest {

    @Test
    void rejectsEachInvalidMovementField() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        List<Runnable> invalidMovements = List.of(
                () -> new Movement("id", null, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), "PEN", null, createdAt),
                () -> new Movement("id", productId, null, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), "PEN", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, null,
                        new BigDecimal("1.00"), "PEN", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        null, "PEN", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("0.00"), "PEN", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.001"), "PEN", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), null, null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), "pen", null, createdAt),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), "PEN", null, null),
                () -> new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                        new BigDecimal("1.00"), "PEN", "x".repeat(201), createdAt));

        // Act
        var failures = invalidMovements.stream()
                .map(invalid -> assertThrows(IllegalArgumentException.class, invalid::run))
                .toList();

        // Assert
        assertEquals(10, failures.size());
    }

    @Test
    void acceptsMinimumAmountAndOptionalDescription() {
        // Arrange
        UUID productId = UUID.randomUUID();

        // Act
        Movement minimum = new Movement("id", productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("0.01"), "PEN", null, Instant.now());
        Movement maximumDescription = new Movement("id-2", productId, ProductType.CREDIT, MovementType.PAYMENT,
                new BigDecimal("1.00"), "USD", "x".repeat(200), Instant.now());

        // Assert
        assertEquals(new BigDecimal("0.01"), minimum.amount());
        assertEquals(200, maximumDescription.description().length());
    }
}
