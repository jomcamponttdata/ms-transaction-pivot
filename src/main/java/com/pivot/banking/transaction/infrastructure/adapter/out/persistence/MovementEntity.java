package com.pivot.banking.transaction.infrastructure.adapter.out.persistence;

import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB representation of a persisted movement. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "movements")
public class MovementEntity {

  @Id private String id;
  private UUID productId;
  private String productType;
  private String movementType;
  private BigDecimal amount;
  private String currency;
  private String description;
  private Instant createdAt;

  /** Converts a domain movement into its MongoDB document. */
  public static MovementEntity fromDomain(Movement movement) {
    return new MovementEntity(
        movement.id(),
        movement.productId(),
        movement.productType().name(),
        movement.movementType().name(),
        movement.amount(),
        movement.currency(),
        movement.description(),
        movement.createdAt());
  }

  /** Reconstructs a domain movement from its MongoDB document. */
  public Movement toDomain() {
    return new Movement(
        id,
        productId,
        ProductType.valueOf(productType),
        MovementType.valueOf(movementType),
        amount,
        currency,
        description,
        createdAt);
  }
}
