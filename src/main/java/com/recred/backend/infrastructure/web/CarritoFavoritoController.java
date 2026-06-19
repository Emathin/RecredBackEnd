package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.SaveCarritoFavorito;
import com.recred.backend.domain.models.CarritoFavorito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/carritos-favoritos")
public class CarritoFavoritoController {

    private final SaveCarritoFavorito saveCarritoFavorito;
    private final GetFavoriteCartsByTutor getFavoriteCartsByTutor;
    private final DeleteFavoriteCart deleteFavoriteCart;

    public CarritoFavoritoController(
            SaveCarritoFavorito saveCarritoFavorito,
            GetFavoriteCartsByTutor getFavoriteCartsByTutor,
            DeleteFavoriteCart deleteFavoriteCart) {
        this.saveCarritoFavorito = saveCarritoFavorito;
        this.getFavoriteCartsByTutor = getFavoriteCartsByTutor;
        this.deleteFavoriteCart = deleteFavoriteCart;
    }

    @PostMapping
    public ResponseEntity<CarritoFavoritoResponse> saveCart(
            Authentication authentication,
            @RequestBody SaveCarritoFavoritoRequest request) {

        Map<UUID, Integer> productQuantities = request.items().stream()
                .collect(Collectors.toMap(
                        SaveCarritoFavoritoRequest.ItemRequest::productId,
                        SaveCarritoFavoritoRequest.ItemRequest::quantity
                ));

        CarritoFavorito cart = saveCarritoFavorito.execute(
                cognitoSubject(authentication),
                request.id(),
                request.nombre(),
                request.alumnoId(),
                productQuantities
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CarritoFavoritoResponse.fromDomain(cart));
    }

    @GetMapping
    public ResponseEntity<List<CarritoFavoritoResponse>> getCarts(
            Authentication authentication,
            @RequestParam(required = false) UUID alumnoId,
            @RequestParam(required = false) UUID productoId) {

        List<CarritoFavorito> carts = getFavoriteCartsByTutor.execute(
                cognitoSubject(authentication),
                alumnoId,
                productoId
        );

        List<CarritoFavoritoResponse> response = carts.stream()
                .map(CarritoFavoritoResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(
            Authentication authentication,
            @PathVariable UUID id) {

        deleteFavoriteCart.execute(cognitoSubject(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
