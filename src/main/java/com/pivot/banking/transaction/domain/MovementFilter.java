package com.pivot.banking.transaction.domain;

import java.time.Instant;
import java.util.UUID;

/** Optional criteria applied when querying movement history. */
public record MovementFilter(UUID productId, ProductType productType, Instant from, Instant to) {
}
