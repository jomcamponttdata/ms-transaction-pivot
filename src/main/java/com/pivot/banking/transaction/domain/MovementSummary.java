package com.pivot.banking.transaction.domain;

import java.math.BigDecimal;

/** Aggregated totals of reported movements sharing a movement type and a currency. */
public record MovementSummary(
    MovementType movementType, String currency, long count, BigDecimal totalAmount) {

  /** Creates a summary after enforcing the invariants of a non-empty monetary aggregate. */
  public MovementSummary {
    if (movementType == null
        || currency == null
        || count < 1
        || totalAmount == null
        || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Movement summary fields do not satisfy the contract");
    }
  }
}
