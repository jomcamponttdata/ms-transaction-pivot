package com.pivot.banking.transaction.infrastructure.adapter.in;

import com.pivot.banking.transaction.adapter.in.dto.MovementRequest;
import com.pivot.banking.transaction.adapter.in.dto.MovementTypeSummary;
import com.pivot.banking.transaction.adapter.in.dto.MovementUpdateRequest;
import com.pivot.banking.transaction.adapter.in.dto.ProductMovementReport;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/** Converts between generated REST DTOs and movement application/domain types. */
@Component
public class MovementRestMapper {

  /** Converts a create request into a movement command. */
  public CreateMovementCommand toCreateCommand(MovementRequest request) {
    return new CreateMovementCommand(
        request.getProductId(),
        com.pivot.banking.transaction.domain.ProductType.valueOf(request.getProductType().name()),
        com.pivot.banking.transaction.domain.MovementType.valueOf(request.getMovementType().name()),
        request.getAmount(),
        request.getCurrency(),
        request.getDescription());
  }

  /** Converts an update request into an application command. */
  public UpdateMovementCommand toUpdateCommand(MovementUpdateRequest request) {
    return new UpdateMovementCommand(
        com.pivot.banking.transaction.domain.MovementType.valueOf(request.getMovementType().name()),
        request.getAmount(),
        request.getCurrency(),
        request.getDescription());
  }

  /** Converts a domain movement into the generated REST response DTO. */
  public com.pivot.banking.transaction.adapter.in.dto.Movement toResponse(
      com.pivot.banking.transaction.domain.Movement movement) {
    return new com.pivot.banking.transaction.adapter.in.dto.Movement(
            movement.id(),
            movement.productId(),
            com.pivot.banking.transaction.adapter.in.dto.ProductType.valueOf(
                movement.productType().name()),
            com.pivot.banking.transaction.adapter.in.dto.MovementType.valueOf(
                movement.movementType().name()),
            movement.amount(),
            movement.currency(),
            movement.createdAt().atOffset(ZoneOffset.UTC))
        .description(movement.description());
  }

  /** Converts a domain report into the generated REST response DTO. */
  public ProductMovementReport toReportResponse(
      com.pivot.banking.transaction.domain.ProductMovementReport report) {
    var movements = report.movements().stream().map(this::toResponse).toList();
    var summary =
        report.summary().stream()
            .map(
                item ->
                    new MovementTypeSummary(
                        com.pivot.banking.transaction.adapter.in.dto.MovementType.valueOf(
                            item.movementType().name()),
                        item.count(),
                        item.totalAmount(),
                        item.currency()))
            .toList();
    return new ProductMovementReport(movements, summary);
  }
}
