package com.pivot.banking.transaction.application.service;

import com.pivot.banking.transaction.adapter.in.dto.MovementRequest;
import com.pivot.banking.transaction.adapter.in.dto.MovementType;
import com.pivot.banking.transaction.adapter.in.dto.MovementUpdateRequest;
import com.pivot.banking.transaction.adapter.in.dto.ProductType;
import com.pivot.banking.transaction.application.port.in.CreateMovement;
import com.pivot.banking.transaction.application.port.in.DeleteMovement;
import com.pivot.banking.transaction.application.port.in.GetMovements;
import com.pivot.banking.transaction.application.port.in.UpdateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementPage;
import com.pivot.banking.transaction.infrastructure.adapter.in.MovementController;
import com.pivot.banking.transaction.infrastructure.adapter.in.MovementRestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementControllerTest {

    @Mock
    private CreateMovement createMovement;
    @Mock
    private GetMovements getMovements;
    @Mock
    private UpdateMovement updateMovement;
    @Mock
    private DeleteMovement deleteMovement;

    private MovementRestMapper mapper = new MovementRestMapper();

    private MovementController controller;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        controller = new MovementController(createMovement, getMovements, updateMovement, deleteMovement, mapper);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldDelegateCreateRequestAndReturnCreatedMovement() {
        // Arrange
        UUID productId = UUID.randomUUID();
        var created = new com.pivot.banking.transaction.domain.Movement("id-1", productId,
                com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT,
                com.pivot.banking.transaction.domain.MovementType.DEPOSIT, new BigDecimal("25.50"), "PEN", "Cash deposit", Instant.now());
        when(createMovement.create(any(CreateMovementCommand.class))).thenReturn(Mono.just(created));

        // Act
        webTestClient.post()
                .uri("/api/v1/movements")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                      "productId": "%s",
                      "productType": "BANK_ACCOUNT",
                      "movementType": "DEPOSIT",
                      "amount": 25.50,
                      "currency": "PEN",
                      "description": "Cash deposit"
                    }
                    """.formatted(productId))
                .exchange()

        // Assert
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/movements/id-1")
                .expectBody()
                .jsonPath("$.id").isEqualTo("id-1")
                .jsonPath("$.productId").isEqualTo(productId.toString())
                .jsonPath("$.productType").isEqualTo("BANK_ACCOUNT")
                .jsonPath("$.movementType").isEqualTo("DEPOSIT")
                .jsonPath("$.amount").isEqualTo(25.50)
                .jsonPath("$.currency").isEqualTo("PEN")
                .jsonPath("$.description").isEqualTo("Cash deposit");

        ArgumentCaptor<CreateMovementCommand> captor = ArgumentCaptor.forClass(CreateMovementCommand.class);
        verify(createMovement, times(1)).create(captor.capture());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT, captor.getValue().productType());
        assertEquals(com.pivot.banking.transaction.domain.MovementType.DEPOSIT, captor.getValue().movementType());
        assertEquals("Cash deposit", captor.getValue().description());
    }

    @Test
    void shouldDelegateFindByIdAndReturnOkMovement() {
        // Arrange
        var existing = new com.pivot.banking.transaction.domain.Movement("id-1", UUID.randomUUID(),
                com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT,
                com.pivot.banking.transaction.domain.MovementType.DEPOSIT, new BigDecimal("10.00"), "PEN",
                null, Instant.parse("2025-01-01T00:00:00Z"));
        when(getMovements.findById("id-1")).thenReturn(Mono.just(existing));

        // Act
        webTestClient.get()
                .uri("/api/v1/movements/id-1")
                .exchange()

        // Assert
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("id-1")
                .jsonPath("$.productType").isEqualTo("BANK_ACCOUNT")
                .jsonPath("$.movementType").isEqualTo("DEPOSIT")
                .jsonPath("$.amount").isEqualTo(10.00)
                .jsonPath("$.currency").isEqualTo("PEN");

        verify(getMovements, times(1)).findById("id-1");
    }

    @Test
    void shouldReturnNotFoundWhenFindByIdDoesNotExist() {
        // Arrange
        when(getMovements.findById("missing")).thenReturn(Mono.empty());

        // Act
        webTestClient.get()
                .uri("/api/v1/movements/missing")
                .exchange()

        // Assert
                .expectStatus().isNotFound();
        verify(getMovements, times(1)).findById("missing");
    }

    @Test
    void shouldDelegateFindAllWithDefaultLimitAndNoCursor() {
        // Arrange
        var first = new com.pivot.banking.transaction.domain.Movement("id-1", UUID.randomUUID(),
                com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT,
                com.pivot.banking.transaction.domain.MovementType.DEPOSIT, new BigDecimal("1.00"), "PEN",
                null, java.time.Instant.parse("2025-01-01T00:00:00Z"));
        var page = new MovementPage(java.util.List.of(first), null);
        when(getMovements.findAll(any(MovementFilter.class), eq(50), eq(null))).thenReturn(Mono.just(page));

        // Act
        webTestClient.get()
                .uri("/api/v1/movements")
                .exchange()

        // Assert
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("id-1")
                .jsonPath("$[0].productType").isEqualTo("BANK_ACCOUNT")
                .jsonPath("$[0].movementType").isEqualTo("DEPOSIT")
                .jsonPath("$[0].amount").isEqualTo(1.00);

        verify(getMovements, times(1)).findAll(any(MovementFilter.class), eq(50), eq(null));
    }

    @Test
    void shouldDelegateFindAllWithExplicitLimitAndCursor() {
        // Arrange
        var first = new com.pivot.banking.transaction.domain.Movement("id-1", UUID.randomUUID(),
                com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT,
                com.pivot.banking.transaction.domain.MovementType.DEPOSIT, new BigDecimal("1.00"), "PEN",
                null, java.time.Instant.parse("2025-01-01T00:00:00Z"));
        var page = new MovementPage(java.util.List.of(first), "next-cursor");
        when(getMovements.findAll(any(MovementFilter.class), eq(10), eq("cursor-token"))).thenReturn(Mono.just(page));

        // Act
        webTestClient.get()
                .uri("/api/v1/movements?limit=10&cursor=cursor-token")
                .exchange()

        // Assert
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Next-Cursor", "next-cursor")
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("id-1");

        verify(getMovements, times(1)).findAll(any(MovementFilter.class), eq(10), eq("cursor-token"));
    }

    @Test
    void shouldRejectInvalidLimitBeforeQueryingRepository() {
        // Arrange
        // Act
        webTestClient.get()
                .uri("/api/v1/movements?limit=0")
                .exchange()

        // Assert
                .expectStatus().isBadRequest();
        verify(getMovements, never()).findAll(any(MovementFilter.class), any(Integer.class), any());
    }

    @Test
    void shouldRejectInvertedDateRange() {
        // Arrange
        // Act
        webTestClient.get()
                .uri("/api/v1/movements?from=2025-02-01T00:00:00Z&to=2025-01-01T00:00:00Z")
                .exchange()

        // Assert
                .expectStatus().isBadRequest();
        verify(getMovements, never()).findAll(any(MovementFilter.class), any(Integer.class), any());
    }

    @Test
    void shouldDelegateUpdateAndReturnUpdatedMovement() {
        // Arrange
        var existing = new com.pivot.banking.transaction.domain.Movement("id-1", UUID.randomUUID(),
                com.pivot.banking.transaction.domain.ProductType.BANK_ACCOUNT,
                com.pivot.banking.transaction.domain.MovementType.DEPOSIT, new BigDecimal("10.00"), "PEN", null, Instant.parse("2025-01-01T00:00:00Z"));
        var updated = existing.update(com.pivot.banking.transaction.domain.MovementType.PAYMENT,
                new BigDecimal("10.00"), "USD", "Updated");
        when(getMovements.findById("id-1")).thenReturn(Mono.just(existing));
        when(updateMovement.update(eq("id-1"), any(UpdateMovementCommand.class))).thenReturn(Mono.just(updated));

        // Act
        webTestClient.put()
                .uri("/api/v1/movements/id-1")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                      "movementType": "PAYMENT",
                      "amount": 10.00,
                      "currency": "USD",
                      "description": "Updated"
                    }
                    """)
                .exchange()

        // Assert
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("id-1")
                .jsonPath("$.movementType").isEqualTo("PAYMENT")
                .jsonPath("$.currency").isEqualTo("USD")
                .jsonPath("$.description").isEqualTo("Updated");

        verify(updateMovement, times(1)).update(eq("id-1"), any(UpdateMovementCommand.class));
    }

    @Test
    void shouldDelegateDeleteAndReturnNoContent() {
        // Arrange
        when(deleteMovement.delete("id-1")).thenReturn(Mono.empty());

        // Act
        webTestClient.delete()
                .uri("/api/v1/movements/id-1")
                .exchange()

        // Assert
                .expectStatus().isNoContent();
        verify(deleteMovement, times(1)).delete("id-1");
    }

    @Test
    void shouldReturnNotFoundWhenDeleteMissingMovement() {
        // Arrange
        when(deleteMovement.delete("missing")).thenReturn(Mono.error(
                new com.pivot.banking.transaction.application.exception.MovementNotFoundException("missing")));

        // Act
        webTestClient.delete()
                .uri("/api/v1/movements/missing")
                .exchange()

        // Assert
                .expectStatus().isNotFound();
        verify(deleteMovement, times(1)).delete("missing");
    }
}