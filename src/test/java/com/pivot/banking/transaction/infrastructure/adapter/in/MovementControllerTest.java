package com.pivot.banking.transaction.infrastructure.adapter.in;

import com.pivot.banking.transaction.application.exception.MovementNotFoundException;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovementCommand;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.DeleteMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.GetMovements;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovementCommand;
import com.pivot.banking.transaction.domain.Movement;
import com.pivot.banking.transaction.domain.MovementFilter;
import com.pivot.banking.transaction.domain.MovementPage;
import com.pivot.banking.transaction.domain.MovementType;
import com.pivot.banking.transaction.domain.ProductType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
    @Spy
    private MovementRestMapper mapper = new MovementRestMapper();
    @InjectMocks
    private MovementController controller;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new MovementExceptionHandler())
                .build();
    }

    @Test
    void shouldDelegateCreateRequestAndReturnCreatedMovement() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Movement created = movement("id-1", productId, "25.50");
        when(createMovement.create(any(CreateMovementCommand.class))).thenReturn(Single.just(created));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/movements")
                .contentType(MediaType.APPLICATION_JSON)
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
                .exchange();

        // Assert
        response.expectStatus().isCreated()
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
        verify(createMovement).create(captor.capture());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(ProductType.BANK_ACCOUNT, captor.getValue().productType());
        assertEquals(MovementType.DEPOSIT, captor.getValue().movementType());
        assertEquals(new BigDecimal("25.50"), captor.getValue().amount());
        assertEquals("PEN", captor.getValue().currency());
        assertEquals("Cash deposit", captor.getValue().description());
    }

    @Test
    void shouldDelegateFindByIdAndReturnOkMovement() {
        // Arrange
        Movement existing = movement("id-1", UUID.randomUUID(), "10.00");
        when(getMovements.findById("id-1")).thenReturn(Single.just(existing));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements/id-1")
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("id-1")
                .jsonPath("$.productType").isEqualTo("BANK_ACCOUNT")
                .jsonPath("$.movementType").isEqualTo("DEPOSIT")
                .jsonPath("$.amount").isEqualTo(10.00)
                .jsonPath("$.currency").isEqualTo("PEN");
        verify(getMovements).findById("id-1");
    }

    @Test
    void shouldReturnNotFoundWhenFindByIdDoesNotExist() {
        // Arrange
        when(getMovements.findById("missing"))
                .thenReturn(Single.error(new MovementNotFoundException("missing")));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements/missing")
                .exchange();

        // Assert
        response.expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Movement not found")
                .jsonPath("$.status").isEqualTo(404);
        verify(getMovements).findById("missing");
    }

    @Test
    void shouldDelegateFindAllWithDefaultLimitAndNoCursor() {
        // Arrange
        Movement first = movement("id-1", UUID.randomUUID(), "1.00");
        MovementPage page = new MovementPage(List.of(first), null);
        when(getMovements.findAll(any(MovementFilter.class), eq(50), isNull()))
                .thenReturn(Single.just(page));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements")
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("id-1")
                .jsonPath("$[0].productType").isEqualTo("BANK_ACCOUNT")
                .jsonPath("$[0].movementType").isEqualTo("DEPOSIT")
                .jsonPath("$[0].amount").isEqualTo(1.00);
        verify(getMovements).findAll(any(MovementFilter.class), eq(50), isNull());
    }

    @Test
    void shouldDelegateFindAllWithExplicitLimitAndCursor() {
        // Arrange
        Movement first = movement("id-1", UUID.randomUUID(), "1.00");
        MovementPage page = new MovementPage(List.of(first), "next-cursor");
        when(getMovements.findAll(any(MovementFilter.class), eq(10), eq("cursor-token")))
                .thenReturn(Single.just(page));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements?limit=10&cursor=cursor-token")
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectHeader().valueEquals("X-Next-Cursor", "next-cursor")
                .expectHeader().doesNotExist("X-Next-Cursor-Invalid")
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("id-1");
        verify(getMovements).findAll(any(MovementFilter.class), eq(10), eq("cursor-token"));
    }

    @Test
    void shouldMapFiltersAndReturnMovementPage() {
        // Arrange
        UUID productId = UUID.randomUUID();
        MovementPage page = new MovementPage(List.of(), null);
        when(getMovements.findAll(any(MovementFilter.class), eq(10), isNull()))
                .thenReturn(Single.just(page));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements?productId={productId}&productType=CREDIT"
                        + "&from=2025-01-01T00:00:00Z&to=2025-01-31T00:00:00Z&limit=10", productId)
                .exchange();

        // Assert
        response.expectStatus().isOk().expectBody().json("[]");
        ArgumentCaptor<MovementFilter> captor = ArgumentCaptor.forClass(MovementFilter.class);
        verify(getMovements).findAll(captor.capture(), eq(10), isNull());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(ProductType.CREDIT, captor.getValue().productType());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), captor.getValue().from());
        assertEquals(Instant.parse("2025-01-31T00:00:00Z"), captor.getValue().to());
    }

    @Test
    void shouldDelegateFindAllWithoutNextCursorHeaderWhenLastPage() {
        // Arrange
        MovementPage page = new MovementPage(List.of(), null);
        when(getMovements.findAll(any(MovementFilter.class), eq(50), isNull()))
                .thenReturn(Single.just(page));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements")
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectHeader().doesNotExist("X-Next-Cursor")
                .expectBody().json("[]");
    }

    @Test
    void shouldPropagateInvalidRequestFromUseCase() {
        // Arrange
        when(getMovements.findAll(any(MovementFilter.class), eq(0), isNull()))
                .thenReturn(Single.error(new IllegalArgumentException("limit must be between 1 and 200")));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.get()
                .uri("/api/v1/movements?limit=0")
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Invalid request");
        verify(getMovements).findAll(any(MovementFilter.class), eq(0), isNull());
    }

    @Test
    void shouldDelegateUpdateAndReturnUpdatedMovement() {
        // Arrange
        Movement updated = movement("id-1", UUID.randomUUID(), "10.00")
                .update(MovementType.PAYMENT, new BigDecimal("10.00"), "USD", "Updated");
        when(updateMovement.update(eq("id-1"), any(UpdateMovementCommand.class)))
                .thenReturn(Single.just(updated));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.put()
                .uri("/api/v1/movements/id-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "movementType": "PAYMENT",
                          "amount": 10.00,
                          "currency": "USD",
                          "description": "Updated"
                        }
                        """)
                .exchange();

        // Assert
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("id-1")
                .jsonPath("$.movementType").isEqualTo("PAYMENT")
                .jsonPath("$.currency").isEqualTo("USD")
                .jsonPath("$.description").isEqualTo("Updated");
        ArgumentCaptor<UpdateMovementCommand> captor = ArgumentCaptor.forClass(UpdateMovementCommand.class);
        verify(updateMovement).update(eq("id-1"), captor.capture());
        assertEquals(MovementType.PAYMENT, captor.getValue().movementType());
        assertEquals("USD", captor.getValue().currency());
        assertEquals("Updated", captor.getValue().description());
    }

    @Test
    void shouldRejectInvalidCreateBodyWithRfc7807Problem() {
        // Arrange
        UUID productId = UUID.randomUUID();

        // Act
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri("/api/v1/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "productId": "%s",
                          "productType": "BANK_ACCOUNT",
                          "movementType": "DEPOSIT",
                          "amount": 0,
                          "currency": "PEN"
                        }
                        """.formatted(productId))
                .exchange();

        // Assert
        response.expectStatus().isBadRequest()
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Invalid request")
                .jsonPath("$.status").isEqualTo(400);
        verify(createMovement, never()).create(any(CreateMovementCommand.class));
    }

    @Test
    void shouldDelegateDeleteAndReturnNoContent() {
        // Arrange
        when(deleteMovement.delete("id-1")).thenReturn(Completable.complete());

        // Act
        WebTestClient.ResponseSpec response = webTestClient.delete()
                .uri("/api/v1/movements/id-1")
                .exchange();

        // Assert
        response.expectStatus().isNoContent();
        verify(deleteMovement).delete("id-1");
    }

    @Test
    void shouldReturnNotFoundWhenDeleteMissingMovement() {
        // Arrange
        when(deleteMovement.delete("missing"))
                .thenReturn(Completable.error(new MovementNotFoundException("missing")));

        // Act
        WebTestClient.ResponseSpec response = webTestClient.delete()
                .uri("/api/v1/movements/missing")
                .exchange();

        // Assert
        response.expectStatus().isNotFound();
        verify(deleteMovement).delete("missing");
    }

    private Movement movement(String id, UUID productId, String amount) {
        return new Movement(id, productId, ProductType.BANK_ACCOUNT, MovementType.DEPOSIT,
                new BigDecimal(amount), "PEN", "Cash deposit", Instant.parse("2025-01-01T00:00:00Z"));
    }
}
