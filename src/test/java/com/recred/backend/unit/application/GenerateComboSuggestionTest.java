package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ComboSuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateComboSuggestionTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private KiosqueroRepository kiosqueroRepository;

    private GenerateComboSuggestion useCase;
    private UUID userId;
    private UUID productId;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        useCase = new GenerateComboSuggestion(productRepository, kiosqueroRepository);
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
    }

    @Test
    void execute_ShouldReturnTop3SuggestedProducts() {
        CategoriaProducto categoryA = createCategory(UUID.randomUUID(), "CatA");
        CategoriaProducto categoryB = createCategory(UUID.randomUUID(), "CatB");

        Producto product = createProduct(productId, "Product A", categoryA);
        
        Producto suggestion1 = createProduct(UUID.randomUUID(), "Product B1", categoryB);
        Producto suggestion2 = createProduct(UUID.randomUUID(), "Product B2", categoryB);
        Producto suggestion3 = createProduct(UUID.randomUUID(), "Product B3", categoryB);

        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findBuffetTop3MostSold(buffetId, categoryA.getId()))
                .thenReturn(List.of(suggestion1, suggestion2, suggestion3));

        ComboSuggestion result = useCase.execute(productId, userId);

        assertEquals(productId, result.getIdProduct());
        assertEquals("Product A", result.getProductName());
        assertEquals(3, result.getSuggestedProducts().size());
        assertEquals(suggestion1.getId(), result.getSuggestedProducts().get(0).getId());
    }

    @Test
    void execute_ShouldThrowException_WhenBuffetNotFound() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> useCase.execute(productId, userId));
    }

    @Test
    void execute_ShouldThrowException_WhenProductNotFound() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> useCase.execute(productId, userId));
    }

    private Producto createProduct(UUID id, String name, CategoriaProducto category) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(name);
        p.setPrecio(BigDecimal.TEN);
        p.setCategoria(category);
        return p;
    }

    private CategoriaProducto createCategory(UUID id, String description) {
        CategoriaProducto c = new CategoriaProducto();
        c.setId(id);
        c.setDescripcion(description);
        return c;
    }
}
