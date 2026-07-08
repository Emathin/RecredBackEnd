package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.application.GenerateComboSuggestionRequest;
import com.recred.backend.application.GenerateComboSuggestionResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateComboSuggestionTest {

    private static final String COGNITO_SUBJECT = "test-sub";
    private static final String PRODUCT_NAME = "Product A";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private KiosqueroRepository kiosqueroRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private GenerateComboSuggestion generateComboSuggestion;
    private GenerateComboSuggestionRequest comboSuggestionRequest;
    private GenerateComboSuggestionResponse comboSuggestionResponse;
    private Throwable thrownException;
    private UUID userId;
    private UUID productId;
    private UUID buffetId;
    private CategoriaProducto category;
    private Producto product;
    private Producto firstSuggestion;

    @BeforeEach
    void setUp() {
        generateComboSuggestion = new GenerateComboSuggestion(
                productRepository,
                kiosqueroRepository,
                usuarioRepository
        );
    }

    @Test
    void givenKiosqueroProductAndTopSoldProducts_whenGeneratingCombo_thenReturnsSuggestions() {
        givenProductWithCategory();
        givenFirstSuggestedProduct();
        givenAuthenticatedKiosqueroWithBuffet();
        givenProductExists();
        givenTopSoldProductsExist();

        whenGenerateComboSuggestion();

        thenComboSuggestionWasReturned();
    }

    @Test
    void givenMissingUser_whenGeneratingCombo_thenThrowsException() {
        givenComboSuggestionRequest();
        givenAuthenticatedUserDoesNotExist();

        whenGenerateComboSuggestionFails(RuntimeException.class);

        thenErrorMessageIs("User not found");
    }

    @Test
    void givenUserWithoutBuffet_whenGeneratingCombo_thenThrowsException() {
        givenComboSuggestionRequest();
        givenAuthenticatedUserExists();
        givenBuffetDoesNotExistForUser();

        whenGenerateComboSuggestionFails(RuntimeException.class);

        thenErrorMessageIs("Buffet not found for user");
    }

    @Test
    void givenMissingProduct_whenGeneratingCombo_thenThrowsException() {
        givenComboSuggestionRequest();
        givenAuthenticatedKiosqueroWithBuffet();
        givenProductDoesNotExist();

        whenGenerateComboSuggestionFails(RuntimeException.class);

        thenErrorMessageIs("Product not found");
    }

    private void givenProductWithCategory() {
        givenComboSuggestionRequest();
        category = category("CatA");
        product = product(productId, PRODUCT_NAME, category);
    }

    private void givenFirstSuggestedProduct() {
        firstSuggestion = product(randomUUID(), "Product B1", category("CatB"));
    }

    private void givenComboSuggestionRequest() {
        productId = randomUUID();
        comboSuggestionRequest = new GenerateComboSuggestionRequest(
                productId,
                COGNITO_SUBJECT
        );
    }

    private void givenAuthenticatedKiosqueroWithBuffet() {
        givenAuthenticatedUserExists();
        givenBuffetExistsForUser();
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenBuffetExistsForUser() {
        buffetId = randomUUID();
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
    }

    private void givenBuffetDoesNotExistForUser() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);
    }

    private void givenProductExists() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void givenProductDoesNotExist() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
    }

    private void givenTopSoldProductsExist() {
        Producto secondSuggestion = product(randomUUID(), "Product B2", category("CatB"));
        Producto thirdSuggestion = product(randomUUID(), "Product B3", category("CatB"));
        when(productRepository.findBuffetTop3MostSold(buffetId, category.getId()))
                .thenReturn(List.of(firstSuggestion, secondSuggestion, thirdSuggestion));
    }

    private void whenGenerateComboSuggestion() {
        comboSuggestionResponse = generateComboSuggestion.execute(comboSuggestionRequest);
    }

    private <T extends Throwable> void whenGenerateComboSuggestionFails(
            Class<T> expectedException) {
        Executable execution = () -> generateComboSuggestion.execute(comboSuggestionRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenComboSuggestionWasReturned() {
        assertEquals(productId, comboSuggestionResponse.idProduct());
        assertEquals(PRODUCT_NAME, comboSuggestionResponse.productName());
        assertEquals(3, comboSuggestionResponse.suggestedProducts().size());
        assertEquals(
                firstSuggestion.getId(),
                comboSuggestionResponse.suggestedProducts().get(0).id());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Producto product(UUID id, String name, CategoriaProducto category) {
        Producto product = new Producto();
        product.setId(id);
        product.setNombre(name);
        product.setPrecio(BigDecimal.TEN);
        product.setCategoria(category);
        return product;
    }

    private CategoriaProducto category(String description) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(randomUUID());
        category.setDescripcion(description);
        return category;
    }
}
