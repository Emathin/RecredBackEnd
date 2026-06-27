package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DeleteCarritoFavorito;
import com.recred.backend.application.DeleteCarritoFavoritoRequest;
import com.recred.backend.application.GetCarritosFavoritosByTutor;
import com.recred.backend.application.GetCarritosFavoritosByTutorRequest;
import com.recred.backend.application.GetCarritosFavoritosByTutorResponse;
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
    private final GetCarritosFavoritosByTutor getCarritosFavoritosByTutor;
    private final DeleteCarritoFavorito deleteCarritoFavorito;

    public CarritoFavoritoController(
            SaveCarritoFavorito saveCarritoFavorito,
            GetCarritosFavoritosByTutor getCarritosFavoritosByTutor,
            DeleteCarritoFavorito deleteCarritoFavorito) {
        this.saveCarritoFavorito = saveCarritoFavorito;
        this.getCarritosFavoritosByTutor = getCarritosFavoritosByTutor;
        this.deleteCarritoFavorito = deleteCarritoFavorito;
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

        List<GetCarritosFavoritosByTutorResponse> carts = getCarritosFavoritosByTutor.execute(
                new GetCarritosFavoritosByTutorRequest(
                        cognitoSubject(authentication),
                        alumnoId,
                        productoId
                )
        );

        List<CarritoFavoritoResponse> response = carts.stream()
                .map(CarritoFavoritoResponse::fromDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(
            Authentication authentication,
            @PathVariable UUID id) {

        deleteCarritoFavorito.execute(
                new DeleteCarritoFavoritoRequest(cognitoSubject(authentication), id)
        );
        return ResponseEntity.noContent().build();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
