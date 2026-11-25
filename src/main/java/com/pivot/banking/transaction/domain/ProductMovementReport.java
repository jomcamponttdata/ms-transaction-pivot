package com.pivot.banking.transaction.domain;

import java.util.List;

/** Reported movements together with their per-type and per-currency totals. */
public record ProductMovementReport(List<Movement> movements, List<MovementSummary> summary) {

  /** Copies both lists to keep the report immutable. */
  public ProductMovementReport {
    movements = List.copyOf(movements);
    summary = List.copyOf(summary);
  }
}
