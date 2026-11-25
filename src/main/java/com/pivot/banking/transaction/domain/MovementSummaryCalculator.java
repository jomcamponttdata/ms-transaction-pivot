package com.pivot.banking.transaction.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pure aggregation of reported movements into per-type and per-currency totals. */
public final class MovementSummaryCalculator {

  private MovementSummaryCalculator() {}

  /**
   * Groups movements by movement type and currency, keeping the order in which each group first
   * appears. Amounts are summed with BigDecimal arithmetic, so no precision is lost and no currency
   * conversion occurs.
   */
  public static List<MovementSummary> summarize(List<Movement> movements) {
    Map<SummaryKey, List<Movement>> grouped =
        movements.stream()
            .collect(
                Collectors.groupingBy(
                    movement -> new SummaryKey(movement.movementType(), movement.currency()),
                    LinkedHashMap::new,
                    Collectors.toList()));

    return grouped.entrySet().stream()
        .map(
            entry ->
                new MovementSummary(
                    entry.getKey().movementType(),
                    entry.getKey().currency(),
                    entry.getValue().size(),
                    entry.getValue().stream()
                        .map(Movement::amount)
                        .collect(Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
        .toList();
  }

  private record SummaryKey(MovementType movementType, String currency) {}
}
