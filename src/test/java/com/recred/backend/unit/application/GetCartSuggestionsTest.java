package com.recred.backend.unit.application;

import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetCartSuggestionsRequest;
import com.recred.backend.application.GetCartSuggestionsResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CartProductAffinity;
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

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCartSuggestionsTest {

    private static final int DEFAULT_LIMIT = 3;

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
    void rankCartAffinitiesBeforeDetectedPreferencesAndBuffetAffinities() {
        RankingScenario scenario = rankingScenario();
        givenRankingSignals(scenario);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(rankingRequest(scenario));

        assertRankingOrder(result, scenario);
    }

    @Test
    void excludeProductsAlreadyInCart() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto inCart = product("Papas");

        givenBaseScenario(alumnoId, buffetId, inCart);
        givenPurchases(alumnoId, purchase(alumnoId, inCart));
        givenStudentAffinities(alumnoId, buffetId, Set.of(inCart.getId()));
        givenBuffetAffinities(buffetId, Set.of(inCart.getId()));
        givenDetectedPreferences(alumnoId);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(request(
                alumnoId,
                buffetId,
                Set.of(inCart.getId()),
                5
        ));

        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void excludeProductsWithoutPositiveStock() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto noStock = product("Barrita");
        noStock.setStockActual(0);
        Producto unknownStock = product("Yogur");
        unknownStock.setStockActual(null);

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                List.of(noStock, unknownStock),
                purchase(alumnoId, noStock),
                purchase(alumnoId, unknownStock));

        GetCartSuggestionsResponse result = execute(alumnoId, buffetId, Set.of(), 5);

        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void execute_WhenAlumnoDoesNotExist_ShouldThrowException() {
        UUID alumnoId = randomUUID();
        givenStudentDoesNotExist(alumnoId);

        assertThrows(RuntimeException.class,
                () -> getCartSuggestions.execute(request(
                        alumnoId,
                        randomUUID(),
                        Set.of(),
                        3)));

        verifyNoInteractions(productRepository, purchaseRepository, sugerenciaIARepository);
    }

    @Test
    void execute_WhenNoProductsAreAvailable_ShouldReturnEmptyList() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        givenStudentExists(alumnoId, student(alumnoId));
        givenAvailableProducts(buffetId);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(request(
                alumnoId,
                buffetId,
                Set.of(),
                3
        ));

        assertTrue(result.suggestions().isEmpty());
        verifyNoInteractions(purchaseRepository, sugerenciaIARepository);
    }

    @Test
    void execute_WithNullOptionalData_ShouldUseFallbackCollections() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto favorite = product("Agua");
        Alumno alumno = student(alumnoId);
        alumno.setProductosBloqueados(null);
        alumno.setProductosFavoritos(Set.of(favorite));

        givenBaseScenario(alumnoId, buffetId, alumno, favorite);
        givenNullOptionalSignals(alumnoId, buffetId);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(
                request(alumnoId, buffetId, null, null));

        assertSuggestion(result, 0, favorite, "FAVORITE");
        assertEquals("Esta entre los favoritos del alumno", result.suggestions().get(0).reason());
    }

    @Test
    void execute_WhenOnlyPurchaseHistoryMatches_ShouldUsePurchaseHistoryReason() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto frequent = product("Jugo");
        LocalDateTime yesterday = now().minusDays(1).atStartOfDay();

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                List.of(frequent),
                purchase(alumnoId, frequent, yesterday));

        GetCartSuggestionsResponse result = execute(alumnoId, buffetId, Set.of(), 0);

        assertSuggestion(result, 0, frequent, "PURCHASE_HISTORY");
        assertEquals("El alumno lo compra con frecuencia", result.suggestions().get(0).reason());
    }

    @Test
    void execute_WhenPurchaseMatchesToday_ShouldUseDayPatternReason() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto product = product("Ensalada");
        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                List.of(product),
                purchase(alumnoId, product, LocalDateTime.now()));

        GetCartSuggestionsResponse result = execute(alumnoId, buffetId, Set.of(), 1);

        assertSuggestion(result, 0, product, "DAY_PATTERN");
        assertEquals("Suele comprarse en dias como hoy", result.suggestions().get(0).reason());
    }

    @Test
    void execute_ShouldIgnoreProductsWithoutId() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto productWithoutId = product("Sin id");
        productWithoutId.setId(null);

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                productWithoutId);

        GetCartSuggestionsResponse result = executeDefault(alumnoId, buffetId);

        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void execute_ShouldIgnoreStudentAffinitiesWithoutProductId() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto complement = product("Alfajor");

        givenBaseScenario(alumnoId, buffetId, complement);
        givenPurchases(alumnoId);
        givenStudentAffinities(
                alumnoId,
                buffetId,
                Set.of(),
                new CartProductAffinity(null, 99),
                new CartProductAffinity(complement.getId(), 1));
        givenBuffetAffinities(buffetId, Set.of());
        givenDetectedPreferences(alumnoId);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(
                request(alumnoId, buffetId, Set.of(), 3));

        assertSuggestion(result, 0, complement, "STUDENT_CART_AFFINITY");
        assertEquals(20, result.suggestions().get(0).score());
    }

    @Test
    void execute_ShouldMergeDuplicateStudentAffinities() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto complement = product("Alfajor");

        givenBaseScenario(alumnoId, buffetId, complement);
        givenPurchases(alumnoId);
        givenStudentAffinities(
                alumnoId,
                buffetId,
                Set.of(),
                new CartProductAffinity(complement.getId(), 1),
                new CartProductAffinity(complement.getId(), 2));
        givenBuffetAffinities(buffetId, Set.of());
        givenDetectedPreferences(alumnoId);

        GetCartSuggestionsResponse result = getCartSuggestions.execute(
                request(alumnoId, buffetId, Set.of(), 3));

        assertSuggestion(result, 0, complement, "STUDENT_CART_AFFINITY");
        assertEquals(60, result.suggestions().get(0).score());
    }

    @Test
    void execute_ShouldFilterBlockedProducts() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto blocked = product("Bloqueado");
        Producto frequent = product("Jugo");
        Alumno alumno = student(alumnoId);
        alumno.setProductosBloqueados(Set.of(blocked));

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                alumno,
                List.of(blocked, frequent),
                purchase(alumnoId, frequent, null));

        GetCartSuggestionsResponse result = executeDefault(alumnoId, buffetId);

        assertSuggestion(result, 0, frequent, "PURCHASE_HISTORY");
    }

    @Test
    void execute_ShouldIgnoreCandidatesWithoutSignals() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto withoutSignals = product("Pan");

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                withoutSignals);

        GetCartSuggestionsResponse result = executeDefault(alumnoId, buffetId);

        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void execute_WhenScoresTie_ShouldOrderByName() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto banana = product("Banana");
        Producto apple = product("apple");
        Alumno alumno = student(alumnoId);
        alumno.setProductosFavoritos(Set.of(banana, apple));

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                alumno,
                List.of(banana, apple));

        GetCartSuggestionsResponse result = executeDefault(alumnoId, buffetId);

        assertSuggestion(result, 0, apple, "FAVORITE");
        assertSuggestion(result, 1, banana, "FAVORITE");
    }

    @Test
    void execute_ShouldAllowDuplicateAvailableIds() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        Producto duplicated = product("Duplicado A");
        Producto duplicatedCopy = product("Duplicado B");
        duplicatedCopy.setId(duplicated.getId());
        Alumno alumno = student(alumnoId);
        alumno.setProductosFavoritos(Set.of(duplicated));

        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                alumno,
                List.of(duplicated, duplicatedCopy));

        GetCartSuggestionsResponse result = executeDefault(alumnoId, buffetId);

        assertSuggestion(result, 0, duplicated, "FAVORITE");
    }

    private GetCartSuggestionsRequest request(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds,
            Integer limit
    ) {
        return new GetCartSuggestionsRequest(alumnoId, buffetId, cartProductIds, limit);
    }

    private GetCartSuggestionsResponse executeDefault(UUID alumnoId, UUID buffetId) {
        return execute(alumnoId, buffetId, Set.of(), DEFAULT_LIMIT);
    }

    private GetCartSuggestionsResponse execute(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds,
            Integer limit) {
        return getCartSuggestions.execute(request(alumnoId, buffetId, cartProductIds, limit));
    }

    private RankingScenario rankingScenario() {
        Producto detectedPreference = product("Sandwich");
        Producto inCart = product("Papas");
        Producto cartComplement = product("Coca Cola");
        Producto buffetComplement = product("Jugo");
        return new RankingScenario(
                randomUUID(),
                randomUUID(),
                detectedPreference,
                inCart,
                cartComplement,
                buffetComplement
        );
    }

    private void givenRankingSignals(RankingScenario scenario) {
        givenStudentExists(scenario.alumnoId(), student(scenario.alumnoId()));
        givenAvailableProducts(
                scenario.buffetId(),
                scenario.detectedPreference(),
                scenario.inCart(),
                scenario.cartComplement(),
                scenario.buffetComplement());
        givenPurchases(
                scenario.alumnoId(),
                purchase(scenario.alumnoId(), scenario.detectedPreference()),
                purchase(scenario.alumnoId(), scenario.detectedPreference()),
                purchase(scenario.alumnoId(), scenario.cartComplement()),
                purchase(scenario.alumnoId(), scenario.buffetComplement()));
        givenDetectedPreferences(
                scenario.alumnoId(),
                scenario.detectedPreference().getId());
        givenStudentAffinities(
                scenario.alumnoId(),
                scenario.buffetId(),
                scenario.cartIds(),
                new CartProductAffinity(scenario.cartComplement().getId(), 3));
        givenBuffetAffinities(
                scenario.buffetId(),
                scenario.cartIds(),
                new CartProductAffinity(scenario.buffetComplement().getId(), 2));
    }

    private GetCartSuggestionsRequest rankingRequest(RankingScenario scenario) {
        return request(scenario.alumnoId(), scenario.buffetId(), scenario.cartIds(), 5);
    }

    private void assertRankingOrder(
            GetCartSuggestionsResponse result,
            RankingScenario scenario) {
        assertSuggestion(result, 0, scenario.cartComplement(), "STUDENT_CART_AFFINITY");
        assertSuggestion(result, 1, scenario.detectedPreference(), "DETECTED_PREFERENCE");
        assertSuggestion(result, 2, scenario.buffetComplement(), "BUFFET_CART_AFFINITY");
    }

    private void givenStudentExists(UUID alumnoId, Alumno alumno) {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
    }

    private void givenStudentDoesNotExist(UUID alumnoId) {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());
    }

    private void givenBaseScenario(UUID alumnoId, UUID buffetId, Producto... products) {
        givenBaseScenario(alumnoId, buffetId, student(alumnoId), products);
    }

    private void givenBaseScenario(
            UUID alumnoId,
            UUID buffetId,
            Alumno alumno,
            Producto... products) {
        givenStudentExists(alumnoId, alumno);
        givenAvailableProducts(buffetId, products);
    }

    private void givenScenarioWithEmptySignals(
            UUID alumnoId,
            UUID buffetId,
            Producto product) {
        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                student(alumnoId),
                List.of(product));
    }

    private void givenScenarioWithEmptySignals(
            UUID alumnoId,
            UUID buffetId,
            Alumno alumno,
            List<Producto> products,
            PurchaseResume... purchases) {
        givenBaseScenario(alumnoId, buffetId, alumno, products.toArray(Producto[]::new));
        givenPurchases(alumnoId, purchases);
        givenEmptySignals(alumnoId, buffetId);
    }

    private void givenScenarioWithEmptySignals(
            UUID alumnoId,
            UUID buffetId,
            List<Producto> products,
            PurchaseResume... purchases) {
        givenScenarioWithEmptySignals(
                alumnoId,
                buffetId,
                student(alumnoId),
                products,
                purchases);
    }

    private void givenAvailableProducts(UUID buffetId, Producto... products) {
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(products));
    }

    private void givenPurchases(UUID alumnoId, PurchaseResume... purchases) {
        when(purchaseRepository.findPurchasesByAlumno(alumnoId))
                .thenReturn(List.of(purchases));
    }

    private void givenDetectedPreferences(UUID alumnoId, UUID... productIds) {
        when(sugerenciaIARepository.findProductIdsByAlumnoIdAndTipo(
                alumnoId,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(productIds));
    }

    private void givenStudentAffinities(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartIds,
            CartProductAffinity... affinities) {
        when(purchaseRepository.findStudentCartAffinities(alumnoId, buffetId, cartIds))
                .thenReturn(List.of(affinities));
    }

    private void givenBuffetAffinities(
            UUID buffetId,
            Set<UUID> cartIds,
            CartProductAffinity... affinities) {
        when(purchaseRepository.findBuffetCartAffinities(buffetId, cartIds))
                .thenReturn(List.of(affinities));
    }

    private void givenEmptySignals(UUID alumnoId, UUID buffetId) {
        givenStudentAffinities(alumnoId, buffetId, Set.of());
        givenBuffetAffinities(buffetId, Set.of());
        givenDetectedPreferences(alumnoId);
    }

    private void givenNullOptionalSignals(UUID alumnoId, UUID buffetId) {
        when(purchaseRepository.findPurchasesByAlumno(alumnoId)).thenReturn(null);
        when(purchaseRepository.findStudentCartAffinities(alumnoId, buffetId, Set.of()))
                .thenReturn(null);
        when(purchaseRepository.findBuffetCartAffinities(buffetId, Set.of())).thenReturn(null);
        givenDetectedPreferences(alumnoId);
    }

    private void assertSuggestion(
            GetCartSuggestionsResponse result,
            int index,
            Producto product,
            String source) {
        assertTrue(result.suggestions().size() > index);
        assertEquals(product.getId(), result.suggestions().get(index).productId());
        assertEquals(source, result.suggestions().get(index).source());
    }

    private Alumno student(UUID alumnoId) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        return alumno;
    }

    private Producto product(String name) {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre(name);
        product.setPrecio(BigDecimal.TEN);
        product.setStockActual(10);
        return product;
    }

    private PurchaseResume purchase(UUID alumnoId, Producto product) {
        return purchase(alumnoId, product, LocalDateTime.now());
    }

    private PurchaseResume purchase(UUID alumnoId, Producto product, LocalDateTime date) {
        return new PurchaseResume(
                alumnoId,
                product.getId(),
                product.getNombre(),
                product.getPrecio(),
                date
        );
    }

    private record RankingScenario(
            UUID alumnoId,
            UUID buffetId,
            Producto detectedPreference,
            Producto inCart,
            Producto cartComplement,
            Producto buffetComplement
    ) {

        private Set<UUID> cartIds() {
            return Set.of(inCart.getId());
        }
    }

}
