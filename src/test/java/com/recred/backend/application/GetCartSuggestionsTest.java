package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.CartSuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCartSuggestionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    private GetCartSuggestions getCartSuggestions;

    @BeforeEach
    void setUp() {
        getCartSuggestions = new GetCartSuggestions(
                alumnoRepository,
                productRepository,
                purchaseRepository,
                sugerenciaIARepository
        );
    }

    @Test
    void rankFavoriteAndDetectedPreferenceWithoutRecalculatingAI() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto favorite = product("Agua");
        Producto detectedPreference = product("Sandwich");
        Producto inCart = product("Papas");
        Producto cartComplement = product("Coca Cola");
        Producto frequent = product("Jugo");
        Producto noStock = product("Barrita");
        noStock.setStockActual(0);
        Producto unknownStock = product("Yogur");
        unknownStock.setStockActual(null);

        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setProductosFavoritos(Set.of(favorite));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(
                        favorite,
                        detectedPreference,
                        inCart,
                        cartComplement,
                        frequent,
                        noStock,
                        unknownStock));
        when(purchaseRepository.findPurchasesByAlumno(alumnoId)).thenReturn(List.of(
                purchase(alumnoId, detectedPreference),
                purchase(alumnoId, detectedPreference),
                purchase(alumnoId, cartComplement),
                purchase(alumnoId, frequent),
                purchase(alumnoId, noStock),
                purchase(alumnoId, unknownStock)
        ));
        when(sugerenciaIARepository.findProductIdsByAlumnoIdAndTipo(
                alumnoId,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(detectedPreference.getId()));
        when(purchaseRepository.findStudentCartAffinities(
                alumnoId,
                buffetId,
                Set.of(inCart.getId())))
                .thenReturn(List.of(new CartProductAffinity(cartComplement.getId(), 3)));
        when(purchaseRepository.findBuffetCartAffinities(buffetId, Set.of(inCart.getId())))
                .thenReturn(List.of(new CartProductAffinity(frequent.getId(), 2)));

        List<CartSuggestion> result = getCartSuggestions.execute(
                alumnoId,
                buffetId,
                Set.of(inCart.getId()),
                5
        );

        assertEquals(3, result.size());
        assertEquals(cartComplement.getId(), result.get(0).product().getId());
        assertEquals("STUDENT_CART_AFFINITY", result.get(0).source());
        assertEquals(detectedPreference.getId(), result.get(1).product().getId());
        assertEquals("DETECTED_PREFERENCE", result.get(1).source());
        assertEquals(frequent.getId(), result.get(2).product().getId());
        assertEquals("BUFFET_CART_AFFINITY", result.get(2).source());
        assertTrue(result.stream().noneMatch(s -> s.product().getId().equals(inCart.getId())));
        assertTrue(result.stream().noneMatch(s -> s.product().getId().equals(noStock.getId())));
        assertTrue(result.stream()
                .noneMatch(s -> s.product().getId().equals(unknownStock.getId())));
    }

    private Producto product(String name) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(BigDecimal.TEN);
        product.setStockActual(10);
        return product;
    }

    private PurchaseResume purchase(UUID alumnoId, Producto product) {
        return new PurchaseResume(
                alumnoId,
                product.getId(),
                product.getNombre(),
                product.getPrecio(),
                LocalDateTime.now()
        );
    }

}
