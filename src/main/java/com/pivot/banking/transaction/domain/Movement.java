package com.pivot.banking.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable movement aggregate recorded for a banking product. */
public record Movement(
    String id,
    UUID productId,
    ProductType productType,
    MovementType movementType,
    BigDecimal amount,
    String currency,
    String description,
    Instant createdAt) {

  /** Creates a movement after enforcing the invariants expressed by the API contract. */
  public Movement {
    if (productId == null
        || productType == null
        || movementType == null
        || amount == null
        || amount.compareTo(new BigDecimal("0.01")) < 0
        || amount.remainder(new BigDecimal("0.01")).compareTo(BigDecimal.ZERO) != 0
        || currency == null
        || !currency.matches("[A-Z]{3}")
        || createdAt == null
        || (description != null && description.length() > 200)) {
      throw new IllegalArgumentException("Movement fields do not satisfy the contract");
    }
  }

  /** Returns a copy with the editable movement fields replaced. */
  public Movement update(
      MovementType updatedType,
      BigDecimal updatedAmount,
      String updatedCurrency,
      String updatedDescription) {
    return new Movement(
        id,
        productId,
        productType,
        updatedType,
        updatedAmount,
        updatedCurrency,
        updatedDescription,
        createdAt);
  }
}
