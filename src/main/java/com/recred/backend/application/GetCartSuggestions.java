package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetCartSuggestions {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 3;

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SugerenciaIARepository sugerenciaIARepository;

    public GetCartSuggestions(
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            SugerenciaIARepository sugerenciaIARepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    public GetCartSuggestionsResponse execute(GetCartSuggestionsRequest request) {
        Alumno alumno = findStudent(request.studentId());

        List<Producto> availableProducts = findAvailableProducts(request.buffetId());
        if (availableProducts.isEmpty()) {
            return new GetCartSuggestionsResponse(List.of());
        }

        SuggestionContext context = buildSuggestionContext(request, alumno, availableProducts);
        List<CartSuggestion> suggestions = suggestProducts(availableProducts, context);

        return GetCartSuggestionsResponse.fromDomain(suggestions);
    }

    private Alumno findStudent(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(alumnoId));
    }

    private List<Producto> findAvailableProducts(UUID buffetId) {
        return productRepository.findAllActiveWithStock(buffetId);
    }

    private SuggestionContext buildSuggestionContext(
            GetCartSuggestionsRequest request,
            Alumno alumno,
            List<Producto> availableProducts
    ) {
        UUID alumnoId = request.studentId();
        UUID buffetId = request.buffetId();
        Set<UUID> cartIds = nullableSet(request.cartProductIds());
        List<PurchaseResume> purchases = purchaseRepository.findPurchasesByAlumno(alumnoId);

        return new SuggestionContext(
                normalizeLimit(request.limit()),
                cartIds,
                productIds(alumno.getProductosBloqueados()),
                productIds(alumno.getProductosFavoritos()),
                availableProductsById(availableProducts),
                purchaseFrequency(purchases),
                todayPatternIds(purchases),
                detectedPreferenceIds(alumnoId),
                studentCartAffinities(alumnoId, buffetId, cartIds),
                buffetCartAffinities(buffetId, cartIds)
        );
    }

    private List<CartSuggestion> suggestProducts(
            List<Producto> availableProducts,
            SuggestionContext context
    ) {
        return availableProducts.stream()
                .filter(this::isEligibleProduct)
                .filter(product -> !context.cartIds().contains(product.getId()))
                .filter(product -> !context.blockedIds().contains(product.getId()))
                .map(product -> score(product, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(Candidate::score).reversed()
                        .thenComparing(candidate -> candidate.product().getNombre(),
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(context.limit())
                .map(this::toSuggestion)
                .toList();
    }

    private boolean isEligibleProduct(Producto product) {
        return product.getId() != null && hasPositiveStock(product);
    }

    private CartSuggestion toSuggestion(Candidate candidate) {
        return new CartSuggestion(
                candidate.product(),
                candidate.reason(),
                candidate.source(),
                candidate.score()
        );
    }

    private Map<UUID, Producto> availableProductsById(List<Producto> availableProducts) {
        return availableProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Producto::getId, product -> product, (a, b) -> a));
    }

    private Map<UUID, Long> studentCartAffinities(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartIds
    ) {
        return cartAffinities(
                purchaseRepository.findStudentCartAffinities(alumnoId, buffetId, cartIds));
    }

    private Map<UUID, Long> buffetCartAffinities(UUID buffetId, Set<UUID> cartIds) {
        return cartAffinities(purchaseRepository.findBuffetCartAffinities(buffetId, cartIds));
    }

    private Optional<Candidate> score(Producto product, SuggestionContext context) {
        UUID productId = product.getId();

        if (!context.availableById().containsKey(productId)) {
            return Optional.empty();
        }

        Map<String, Integer> sourceScores = sourceScores(productId, context);
        int totalScore = totalScore(sourceScores);
        if (totalScore == 0) {
            return Optional.empty();
        }

        String source = strongestSource(sourceScores);
        return Optional.of(new Candidate(product, source, reason(source), totalScore));
    }

    private Map<String, Integer> sourceScores(UUID productId, SuggestionContext context) {
        Map<String, Integer> sourceScores = new LinkedHashMap<>();
        addStudentAffinityScore(sourceScores, context.studentCartAffinities().get(productId));
        addBuffetAffinityScore(sourceScores, context.buffetCartAffinities().get(productId));
        addFixedScore(sourceScores, "FAVORITE", context.favoriteIds().contains(productId), 50);
        addFixedScore(
                sourceScores,
                "DETECTED_PREFERENCE",
                context.detectedPreferenceIds().contains(productId),
                40
        );
        addPurchaseHistoryScore(sourceScores, context.purchaseFrequency().get(productId));
        addFixedScore(
                sourceScores,
                "DAY_PATTERN",
                context.todayPatternIds().contains(productId),
                20
        );
        return sourceScores;
    }

    private void addStudentAffinityScore(Map<String, Integer> scores, Long affinity) {
        if (affinity != null && affinity > 0) {
            scores.put("STUDENT_CART_AFFINITY", Math.min(80, affinity.intValue() * 20));
        }
    }

    private void addBuffetAffinityScore(Map<String, Integer> scores, Long affinity) {
        if (affinity != null && affinity > 0) {
            scores.put("BUFFET_CART_AFFINITY", Math.min(60, affinity.intValue() * 15));
        }
    }

    private void addPurchaseHistoryScore(Map<String, Integer> scores, Long frequency) {
        if (frequency != null && frequency > 0) {
            scores.put("PURCHASE_HISTORY", Math.min(30, frequency.intValue() * 5));
        }
    }

    private void addFixedScore(
            Map<String, Integer> scores,
            String source,
            boolean applies,
            int score
    ) {
        if (applies) {
            scores.put(source, score);
        }
    }

    private int totalScore(Map<String, Integer> scores) {
        return scores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private boolean hasPositiveStock(Producto product) {
        return product.getStockDisponible() != null && product.getStockDisponible() > 0;
    }

    private Set<UUID> nullableSet(Set<UUID> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> productIds(Set<Producto> products) {
        if (products == null) {
            return Set.of();
        }
        return products.stream()
                .map(Producto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<UUID, Long> purchaseFrequency(List<PurchaseResume> purchases) {
        if (purchases == null) {
            return Map.of();
        }
        return purchases.stream()
                .map(PurchaseResume::getIdProducto)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, HashMap::new, Collectors.counting()));
    }

    private Map<UUID, Long> cartAffinities(List<CartProductAffinity> affinities) {
        if (affinities == null) {
            return Map.of();
        }
        return affinities.stream()
                .filter(affinity -> affinity.productId() != null)
                .collect(Collectors.toMap(
                        CartProductAffinity::productId,
                        CartProductAffinity::togetherCount,
                        Long::sum));
    }

    private Set<UUID> todayPatternIds(List<PurchaseResume> purchases) {
        if (purchases == null) {
            return Set.of();
        }
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return purchases.stream()
                .filter(purchase -> purchase.getDate() != null)
                .filter(purchase -> purchase.getDate().getDayOfWeek() == today)
                .map(PurchaseResume::getIdProducto)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<UUID> detectedPreferenceIds(UUID alumnoId) {
        return sugerenciaIARepository
                .findProductIdsByAlumnoIdAndTipo(
                        alumnoId,
                        TipoSugerenciaIA.PREFERENCIA_DETECTADA)
                .stream()
                .collect(Collectors.toSet());
    }

    private String strongestSource(Map<String, Integer> sourceScores) {
        return sourceScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("PURCHASE_HISTORY");
    }

    private String reason(String source) {
        return switch (source) {
            case "STUDENT_CART_AFFINITY" -> "Suele comprarse junto con este carrito";
            case "BUFFET_CART_AFFINITY" -> "Es una combinacion frecuente en el buffet";
            case "FAVORITE" -> "Esta entre los favoritos del alumno";
            case "DETECTED_PREFERENCE" -> "Coincide con una preferencia detectada";
            case "DAY_PATTERN" -> "Suele comprarse en dias como hoy";
            default -> "El alumno lo compra con frecuencia";
        };
    }

    private record Candidate(
            Producto product,
            String source,
            String reason,
            int score
    ) {
    }

    private record SuggestionContext(
            int limit,
            Set<UUID> cartIds,
            Set<UUID> blockedIds,
            Set<UUID> favoriteIds,
            Map<UUID, Producto> availableById,
            Map<UUID, Long> purchaseFrequency,
            Set<UUID> todayPatternIds,
            Set<UUID> detectedPreferenceIds,
            Map<UUID, Long> studentCartAffinities,
            Map<UUID, Long> buffetCartAffinities
    ) {
    }
}
