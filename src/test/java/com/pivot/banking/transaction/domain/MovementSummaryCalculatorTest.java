package com.pivot.banking.transaction.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for pure report aggregation by movement type and currency. */
class MovementSummaryCalculatorTest {

  @Test
  void groupsByMovementTypeAndCurrencyWithCountAndTotal() {
    // Arrange
    UUID productId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    List<Movement> movements =
        List.of(
            movement(productId, createdAt, "deposit-pen-1", MovementType.DEPOSIT, "PEN", "10.00"),
            movement(
                productId, createdAt, "withdrawal-pen", MovementType.WITHDRAWAL, "PEN", "2.50"),
            movement(productId, createdAt, "deposit-usd", MovementType.DEPOSIT, "USD", "3.00"),
            movement(productId, createdAt, "deposit-pen-2", MovementType.DEPOSIT, "PEN", "5.00"));

    // Act
    List<MovementSummary> summary = MovementSummaryCalculator.summarize(movements);

    // Assert
    assertEquals(3, summary.size());
    assertEquals(
        new MovementSummary(MovementType.DEPOSIT, "PEN", 2, new BigDecimal("15.00")),
        summary.get(0));
    assertEquals(
        new MovementSummary(MovementType.WITHDRAWAL, "PEN", 1, new BigDecimal("2.50")),
        summary.get(1));
    assertEquals(
        new MovementSummary(MovementType.DEPOSIT, "USD", 1, new BigDecimal("3.00")),
        summary.get(2));
  }

  @Test
  void returnsAnEmptySummaryForNoMovements() {
    // Act
    List<MovementSummary> summary = MovementSummaryCalculator.summarize(List.of());

    // Assert
    assertTrue(summary.isEmpty());
  }

  private Movement movement(
      UUID productId,
      Instant createdAt,
      String id,
      MovementType type,
      String currency,
      String amount) {
    return new Movement(
        id,
        productId,
        ProductType.BANK_ACCOUNT,
        type,
        new BigDecimal(amount),
        currency,
        null,
        createdAt);
  }
}
