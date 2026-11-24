package com.pivot.banking.transaction.infrastructure.adapter.in;

import com.pivot.banking.transaction.adapter.in.api.MovementsApi;
import com.pivot.banking.transaction.adapter.in.dto.Movement;
import com.pivot.banking.transaction.adapter.in.dto.MovementRequest;
import com.pivot.banking.transaction.adapter.in.dto.MovementUpdateRequest;
import com.pivot.banking.transaction.adapter.in.dto.ProductType;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.CreateMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.DeleteMovement;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.GetMovements;
import com.pivot.banking.transaction.application.port.in.MovementUseCases.UpdateMovement;
import com.pivot.banking.transaction.domain.MovementFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Implements the generated REST endpoints for movement history and administration. */
@Controller
public class MovementController implements MovementsApi {

    private final CreateMovement createMovement;
    private final GetMovements getMovements;
    private final UpdateMovement updateMovement;
    private final DeleteMovement deleteMovement;
    private final MovementRestMapper mapper;

    /** Creates the controller with the movement use cases and mapper. */
    public MovementController(CreateMovement createMovement, GetMovements getMovements,
                              UpdateMovement updateMovement, DeleteMovement deleteMovement,
                              MovementRestMapper mapper) {
        this.createMovement = createMovement;
        this.getMovements = getMovements;
        this.updateMovement = updateMovement;
        this.deleteMovement = deleteMovement;
        this.mapper = mapper;
    }

    /** Registers a movement and returns its resource URI. */
    @Override
    public Mono<ResponseEntity<Movement>> createMovement(Mono<MovementRequest> request,
                                                          ServerWebExchange exchange) {
        return request.flatMap(body -> RxJava3Adapter.singleToMono(
                        createMovement.create(mapper.toCreateCommand(body))))
                .map(mapper::toResponse)
                .map(movement -> ResponseEntity.created(URI.create(
                        "/api/v1/movements/" + movement.getId())).body(movement));
    }

    /** Deletes an existing movement and returns no content. */
    @Override
    public Mono<ResponseEntity<Void>> deleteMovement(String movementId, ServerWebExchange exchange) {
        return RxJava3Adapter.completableToMono(deleteMovement.delete(movementId))
                .thenReturn(ResponseEntity.noContent().build());
    }

    /** Returns the requested movement history page and optional next-page header. */
    @Override
    public Mono<ResponseEntity<Flux<Movement>>> findAllMovements(@Nullable UUID productId,
            @Nullable ProductType productType, @Nullable OffsetDateTime from, @Nullable OffsetDateTime to,
            Integer limit, @Nullable String cursor, ServerWebExchange exchange) {
        var type = productType == null ? null
                : com.pivot.banking.transaction.domain.ProductType.valueOf(productType.name());
        var filter = new MovementFilter(productId, type,
                from == null ? null : from.toInstant(), to == null ? null : to.toInstant());
        return RxJava3Adapter.singleToMono(getMovements.findAll(filter, limit, cursor))
                .map(page -> {
                    var response = ResponseEntity.ok();
                    if (page.nextCursor() != null) {
                        response.header("X-Next-Cursor", page.nextCursor());
                    }
                    return response.body(Flux.fromIterable(page.movements()).map(mapper::toResponse));
                });
    }

    /** Returns a movement by its MongoDB document identifier. */
    @Override
    public Mono<ResponseEntity<Movement>> findMovementById(String movementId, ServerWebExchange exchange) {
        return RxJava3Adapter.singleToMono(getMovements.findById(movementId))
                .map(mapper::toResponse).map(ResponseEntity::ok);
    }

    /** Replaces editable movement fields. */
    @Override
    public Mono<ResponseEntity<Movement>> updateMovement(String movementId,
            Mono<MovementUpdateRequest> request, ServerWebExchange exchange) {
        return request.flatMap(body -> RxJava3Adapter.singleToMono(
                        updateMovement.update(movementId, mapper.toUpdateCommand(body))))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
