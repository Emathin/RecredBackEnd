package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.DeleteFavoriteCartRequest;
import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.GetFavoriteCartsByTutorRequest;
import com.recred.backend.application.GetFavoriteCartsByTutorResponse;
import com.recred.backend.application.SaveFavoriteCart;
import com.recred.backend.application.SaveFavoriteCartResponse;
import com.recred.backend.application.SaveFavoriteCartRequest;
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

    private final SaveFavoriteCart saveFavoriteCart;
    private final GetFavoriteCartsByTutor getFavoriteCartsByTutor;
    private final DeleteFavoriteCart deleteFavoriteCart;
    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public CarritoFavoritoController(
            SaveFavoriteCart saveFavoriteCart,
            GetFavoriteCartsByTutor getFavoriteCartsByTutor,
            DeleteFavoriteCart deleteFavoriteCart,
            SchoolLicenseAccessGuard schoolLicenseAccessGuard) {
        this.saveFavoriteCart = saveFavoriteCart;
        this.getFavoriteCartsByTutor = getFavoriteCartsByTutor;
        this.deleteFavoriteCart = deleteFavoriteCart;
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @PostMapping
    public ResponseEntity<CarritoFavoritoResponse> saveCart(
            Authentication authentication,
            @RequestBody SaveCarritoFavoritoRequest request) {

        schoolLicenseAccessGuard.requireActiveSchoolForStudent(request.alumnoId());
        Map<UUID, Integer> productQuantities = request.items().stream()
                .collect(Collectors.toMap(
                        SaveCarritoFavoritoRequest.ItemRequest::productId,
                        SaveCarritoFavoritoRequest.ItemRequest::quantity
                ));

        SaveFavoriteCartResponse cart = saveFavoriteCart.execute(
                new SaveFavoriteCartRequest(
                        cognitoSubject(authentication),
                        request.id(),
                        request.nombre(),
                        request.alumnoId(),
                        productQuantities
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CarritoFavoritoResponse.fromUseCaseResponse(cart));
    }

    @GetMapping
    public ResponseEntity<List<CarritoFavoritoResponse>> getCarts(
            Authentication authentication,
            @RequestParam(required = false) UUID alumnoId,
            @RequestParam(required = false) UUID productoId) {

        if (alumnoId != null) {
            schoolLicenseAccessGuard.requireActiveSchoolForStudent(alumnoId);
        }

        List<GetFavoriteCartsByTutorResponse> carts = getFavoriteCartsByTutor.execute(
                new GetFavoriteCartsByTutorRequest(
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

        deleteFavoriteCart.execute(
                new DeleteFavoriteCartRequest(cognitoSubject(authentication), id)
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
