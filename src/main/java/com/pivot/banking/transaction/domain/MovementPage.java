package com.pivot.banking.transaction.domain;

import java.util.List;

/** A bounded page of movements and its optional continuation cursor. */
public record MovementPage(List<Movement> movements, String nextCursor) {

    /** Copies page results to keep the value immutable. */
    public MovementPage {
        movements = List.copyOf(movements);
    }
}
