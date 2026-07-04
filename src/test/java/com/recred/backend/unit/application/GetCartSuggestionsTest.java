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
import org.junit.jupiter.api.function.Executable;
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

        GetCartSuggestionsResponse result = whenRankingSuggestionsAreRequested(scenario);

        assertEquals(scenario.cartComplement().getId(), result.suggestions().get(0).productId());
        assertEquals("STUDENT_CART_AFFINITY", result.suggestions().get(0).source());
        assertEquals(scenario.detectedPreference().getId(), result.suggestions().get(1).productId());
        assertEquals("DETECTED_PREFERENCE", result.suggestions().get(1).source());
        assertEquals(scenario.buffetComplement().getId(), result.suggestions().get(2).productId());
        assertEquals("BUFFET_CART_AFFINITY", result.suggestions().get(2).source());
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

        GetCartSuggestionsResponse result = whenSuggestionsAreRequested(
                alumnoId,
                buffetId,
                Set.of(inCart.getId()),
                5);

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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), 5);

        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void execute_WhenAlumnoDoesNotExist_ShouldThrowException() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        givenStudentDoesNotExist(alumnoId);

        Executable execution = () -> whenSuggestionsAreRequested(
                alumnoId,
                buffetId,
                Set.of(),
                DEFAULT_LIMIT);

        assertThrows(RuntimeException.class, execution);
        verifyNoInteractions(productRepository, purchaseRepository, sugerenciaIARepository);
    }

    @Test
    void execute_WhenNoProductsAreAvailable_ShouldReturnEmptyList() {
        UUID alumnoId = randomUUID();
        UUID buffetId = randomUUID();
        givenStudentExists(alumnoId, student(alumnoId));
        givenAvailableProducts(buffetId);

        GetCartSuggestionsResponse result = whenSuggestionsAreRequested(
                alumnoId,
                buffetId,
                Set.of(),
                3);

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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, null, null);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(favorite.getId(), result.suggestions().get(0).productId());
        assertEquals("FAVORITE", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), 0);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(frequent.getId(), result.suggestions().get(0).productId());
        assertEquals("PURCHASE_HISTORY", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), 1);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(product.getId(), result.suggestions().get(0).productId());
        assertEquals("DAY_PATTERN", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result = whenDefaultSuggestionsAreRequested(alumnoId, buffetId);

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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), 3);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(complement.getId(), result.suggestions().get(0).productId());
        assertEquals("STUDENT_CART_AFFINITY", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result =
                whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), 3);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(complement.getId(), result.suggestions().get(0).productId());
        assertEquals("STUDENT_CART_AFFINITY", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result = whenDefaultSuggestionsAreRequested(alumnoId, buffetId);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(frequent.getId(), result.suggestions().get(0).productId());
        assertEquals("PURCHASE_HISTORY", result.suggestions().get(0).source());
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

        GetCartSuggestionsResponse result = whenDefaultSuggestionsAreRequested(alumnoId, buffetId);

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

        GetCartSuggestionsResponse result = whenDefaultSuggestionsAreRequested(alumnoId, buffetId);

        assertTrue(result.suggestions().size() > 1);
        assertEquals(apple.getId(), result.suggestions().get(0).productId());
        assertEquals("FAVORITE", result.suggestions().get(0).source());
        assertEquals(banana.getId(), result.suggestions().get(1).productId());
        assertEquals("FAVORITE", result.suggestions().get(1).source());
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

        GetCartSuggestionsResponse result = whenDefaultSuggestionsAreRequested(alumnoId, buffetId);

        assertTrue(result.suggestions().size() > 0);
        assertEquals(duplicated.getId(), result.suggestions().get(0).productId());
        assertEquals("FAVORITE", result.suggestions().get(0).source());
    }

    private GetCartSuggestionsRequest request(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds,
            Integer limit
    ) {
        return new GetCartSuggestionsRequest(alumnoId, buffetId, cartProductIds, limit);
    }

    private GetCartSuggestionsResponse whenDefaultSuggestionsAreRequested(
            UUID alumnoId,
            UUID buffetId) {
        return whenSuggestionsAreRequested(alumnoId, buffetId, Set.of(), DEFAULT_LIMIT);
    }

    private GetCartSuggestionsResponse whenSuggestionsAreRequested(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds,
            Integer limit) {
        return getCartSuggestions.execute(request(alumnoId, buffetId, cartProductIds, limit));
    }

    private GetCartSuggestionsResponse whenRankingSuggestionsAreRequested(
            RankingScenario scenario) {
        return getCartSuggestions.execute(rankingRequest(scenario));
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
