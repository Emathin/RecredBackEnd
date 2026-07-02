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
    @Mock
    private com.recred.backend.domain.repositories.UsuarioRepository usuarioRepository;

    private GenerateComboSuggestion useCase;
    private UUID userId;
    private UUID productId;
    private UUID buffetId;
    private final String cognitoSubject = "test-sub";

    @BeforeEach
    void setUp() {
        useCase = new GenerateComboSuggestion(productRepository, kiosqueroRepository, usuarioRepository);
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

        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(userId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findBuffetTop3MostSold(buffetId, categoryA.getId()))
                .thenReturn(List.of(suggestion1, suggestion2, suggestion3));

        GenerateComboSuggestionRequest request = new GenerateComboSuggestionRequest(productId, cognitoSubject);
        GenerateComboSuggestionResponse result = useCase.execute(request);

        assertEquals(productId, result.idProduct());
        assertEquals("Product A", result.productName());
        assertEquals(3, result.suggestedProducts().size());
        assertEquals(suggestion1.getId(), result.suggestedProducts().get(0).id());
    }

    @Test
    void execute_ShouldThrowException_WhenBuffetNotFound() {
        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(userId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);

        GenerateComboSuggestionRequest request = new GenerateComboSuggestionRequest(productId, cognitoSubject);
        assertThrows(RuntimeException.class, () -> useCase.execute(request));
    }

    @Test
    void execute_ShouldThrowException_WhenProductNotFound() {
        com.recred.backend.domain.models.Usuario usuario = new com.recred.backend.domain.models.Usuario();
        usuario.setId(userId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        GenerateComboSuggestionRequest request = new GenerateComboSuggestionRequest(productId, cognitoSubject);
        assertThrows(RuntimeException.class, () -> useCase.execute(request));
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
