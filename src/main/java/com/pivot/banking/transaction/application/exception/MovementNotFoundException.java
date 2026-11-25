package com.pivot.banking.transaction.application.exception;

/** Signals that the requested movement does not exist. */
public class MovementNotFoundException extends RuntimeException {

  /** Creates the exception for a missing movement identifier. */
  public MovementNotFoundException(String movementId) {
    super("Movement not found: " + movementId);
  }
}
