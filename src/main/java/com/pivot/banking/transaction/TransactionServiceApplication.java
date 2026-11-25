package com.pivot.banking.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the transaction microservice. */
@SpringBootApplication
public class TransactionServiceApplication {

  /** Boots the Spring application context. */
  public static void main(String[] args) {
    SpringApplication.run(TransactionServiceApplication.class, args);
  }
}
