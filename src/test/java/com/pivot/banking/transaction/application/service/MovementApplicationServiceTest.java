package com.pivot.banking.transaction.application.service;

import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import com.pivot.banking.transaction.application.port.out.MovementRepositoryPort;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementCursor;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementApplicationServiceTest {

    @Mock
    private MovementRepositoryPort repository;

    private MovementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MovementApplicationService(repository);
    }

    @Test
    void createUsesTheContractCurrencyDefaultAndPersistsMovement() {
        // Arrange
        var command = new CreateMovementCommand(UUID.randomUUID(), ProductType.BANK_ACCOUNT,
                MovementType.DEPOSIT, new BigDecimal("25.50"), null, "Cash deposit");
        when(repository.save(any(Movement.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        // Act
        var result = service.create(command);

        // Assert
        result.test()
                .assertValue(movement -> movement.id() != null
                        && "PEN".equals(movement.currency())
                        && command.productId().equals(movement.productId())
                        && movement.createdAt() != null)
                .assertComplete();
    }

    @Test
    void findAllBuildsContinuationCursorWhenAnotherItemExists() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);
        var first = movement("first", "2025-01-03T10:00:00Z");
        var second = movement("second", "2025-01-02T10:00:00Z");
        var extra = movement("extra", "2025-01-01T10:00:00Z");
        when(repository.findPage(filter, null, 3)).thenReturn(Flowable.just(first, second, extra));

        // Act
        var firstPage = service.findAll(filter, 2, null);

        // Assert
        firstPage.test()
                .assertValue(page -> page.movements().equals(List.of(first, second))
                        && page.nextCursor() != null)
                .assertComplete();

        var cursorCaptor = ArgumentCaptor.forClass(MovementCursor.class);
        when(repository.findPage(eq(filter), cursorCaptor.capture(), eq(3)))
                .thenReturn(Flowable.just(extra));
        // Act
        var secondPage = service.findAll(filter, 2, "MjAyNS0wMS0wMlQxMDowMDowMFoKc2Vjb25k");

        // Assert
        secondPage.test().assertValue(page -> page.nextCursor() == null).assertComplete();
        assertEquals(second.id(), cursorCaptor.getValue().id());
        assertEquals(second.createdAt(), cursorCaptor.getValue().createdAt());
    }

    @Test
    void findAllRejectsInvalidCursorAndPageSizeBeforeQueryingRepository() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);

        // Act
        var oversizedPage = service.findAll(filter, 201, null);
        var invalidCursor = service.findAll(filter, 10, "bad-cursor");

        // Assert
        oversizedPage.test().assertError(IllegalArgumentException.class);
        invalidCursor.test().assertError(IllegalArgumentException.class);

        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void findAllRejectsAnInvertedDateRange() {
        // Arrange
        var filter = new MovementFilter(null, null, Instant.parse("2025-02-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"));

        // Act
        var result = service.findAll(filter, 10, null);

        // Assert
        result.test().assertError(IllegalArgumentException.class);

        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void updatePreservesProductAndCreationMetadata() {
        // Arrange
        var existing = movement("movement-1", "2025-01-01T00:00:00Z");
        var command = new UpdateMovementCommand(MovementType.PAYMENT, new BigDecimal("10.00"),
                "USD", "Updated");
        when(repository.findById(existing.id())).thenReturn(Maybe.just(existing));
        when(repository.save(any(Movement.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        // Act
        var result = service.update(existing.id(), command);

        // Assert
        result.test().assertValue(updated -> existing.productId().equals(updated.productId())
                        && existing.productType() == updated.productType()
                        && existing.createdAt().equals(updated.createdAt())
                        && MovementType.PAYMENT == updated.movementType()
                        && "USD".equals(updated.currency()))
                .assertComplete();
    }

    @Test
    void deleteReturnsNotFoundForMissingMovement() {
        // Arrange
        when(repository.findById("missing")).thenReturn(Maybe.empty());

        // Act
        var result = service.delete("missing");

        // Assert
        result.test().assertError(MovementNotFoundException.class);

        verify(repository, never()).deleteById("missing");
    }

    @Test
    void createKeepsTheProvidedCurrency() {
        // Arrange
        var command = new CreateMovementCommand(UUID.randomUUID(), ProductType.CREDIT_CARD,
                MovementType.CHARGE, new BigDecimal("35.00"), "USD", "Card charge");
        when(repository.save(any(Movement.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        // Act
        var result = service.create(command);

        // Assert
        result.test().assertValue(movement -> "USD".equals(movement.currency())).assertComplete();
        verify(repository).save(any(Movement.class));
    }

    @Test
    void findAllRejectsANonPositiveLimit() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);

        // Act
        var result = service.findAll(filter, 0, null);

        // Assert
        result.test().assertError(IllegalArgumentException.class);
        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void findAllAcceptsClosedAndOpenDateRanges() {
        // Arrange
        var closedRange = new MovementFilter(null, null, Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T00:00:00Z"));
        var openRange = new MovementFilter(null, null, Instant.parse("2025-01-01T00:00:00Z"), null);
        when(repository.findPage(closedRange, null, 11)).thenReturn(Flowable.empty());
        when(repository.findPage(openRange, null, 11)).thenReturn(Flowable.empty());

        // Act
        var closedResult = service.findAll(closedRange, 10, null);
        var openResult = service.findAll(openRange, 10, null);

        // Assert
        closedResult.test().assertValue(page -> page.nextCursor() == null).assertComplete();
        openResult.test().assertValue(page -> page.nextCursor() == null).assertComplete();
        verify(repository).findPage(closedRange, null, 11);
        verify(repository).findPage(openRange, null, 11);
    }

    @Test
    void findAllTreatsABlankCursorAsTheFirstPage() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);
        when(repository.findPage(filter, null, 11)).thenReturn(Flowable.empty());

        // Act
        var result = service.findAll(filter, 10, " ");

        // Assert
        result.test().assertValue(page -> page.nextCursor() == null).assertComplete();
        verify(repository).findPage(filter, null, 11);
    }

    @Test
    void findAllRejectsMalformedDecodedCursorContents() {
        // Arrange
        var filter = new MovementFilter(null, null, null, null);
        var missingSeparator = cursorToken("2025-01-01T00:00:00Z");
        var missingId = cursorToken("2025-01-01T00:00:00Z\n");
        var invalidTimestamp = cursorToken("not-a-date\nmovement-id");

        // Act
        var separatorResult = service.findAll(filter, 10, missingSeparator);
        var missingIdResult = service.findAll(filter, 10, missingId);
        var timestampResult = service.findAll(filter, 10, invalidTimestamp);

        // Assert
        separatorResult.test().assertError(IllegalArgumentException.class);
        missingIdResult.test().assertError(IllegalArgumentException.class);
        timestampResult.test().assertError(IllegalArgumentException.class);
        verify(repository, never()).findPage(any(), any(), any(Integer.class));
    }

    @Test
    void deleteRemovesAnExistingMovement() {
        // Arrange
        var existing = movement("movement-1", "2025-01-01T00:00:00Z");
        when(repository.findById(existing.id())).thenReturn(Maybe.just(existing));
        when(repository.deleteById(existing.id())).thenReturn(io.reactivex.rxjava3.core.Completable.complete());

        // Act
        var result = service.delete(existing.id());

        // Assert
        result.test().assertComplete();
        verify(repository).deleteById(existing.id());
    }

    private String cursorToken(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Movement movement(String id, String createdAt) {
        return new Movement(id, UUID.randomUUID(), ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal("1.00"), "PEN", null, Instant.parse(createdAt));
    }
}
