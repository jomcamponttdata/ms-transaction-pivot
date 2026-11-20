package com.pivot.banking.transaction.domain;

import java.time.Instant;

/** Position marker for stable descending movement pagination. */
public record MovementCursor(Instant createdAt, String id) {
}
