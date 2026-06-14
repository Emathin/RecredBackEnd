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

    public List<CartSuggestion> execute(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds,
            Integer requestedLimit) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(alumnoId));

        List<Producto> availableProducts = productRepository.findAllActiveWithStock(buffetId);
        if (availableProducts.isEmpty()) {
            return List.of();
        }

        int limit = normalizeLimit(requestedLimit);
        Set<UUID> cartIds = nullableSet(cartProductIds);
        Set<UUID> blockedIds = productIds(alumno.getProductosBloqueados());
        Set<UUID> favoriteIds = productIds(alumno.getProductosFavoritos());
        Map<UUID, Producto> availableById = availableProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Producto::getId, product -> product, (a, b) -> a));

        List<PurchaseResume> purchases = purchaseRepository.findPurchasesByAlumno(alumnoId);
        Map<UUID, Long> purchaseFrequency = purchaseFrequency(purchases);
        Set<UUID> todayPatternIds = todayPatternIds(purchases);
        Set<UUID> detectedPreferenceIds = detectedPreferenceIds(alumnoId);
        Map<UUID, Long> studentCartAffinities = cartAffinities(
                purchaseRepository.findStudentCartAffinities(alumnoId, buffetId, cartIds));
        Map<UUID, Long> buffetCartAffinities = cartAffinities(
                purchaseRepository.findBuffetCartAffinities(buffetId, cartIds));

        return availableProducts.stream()
                .filter(product -> product.getId() != null)
                .filter(this::hasPositiveStock)
                .filter(product -> !cartIds.contains(product.getId()))
                .filter(product -> !blockedIds.contains(product.getId()))
                .map(product -> score(
                        product,
                        favoriteIds,
                        detectedPreferenceIds,
                        purchaseFrequency,
                        todayPatternIds,
                        studentCartAffinities,
                        buffetCartAffinities,
                        availableById))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(Candidate::score).reversed()
                        .thenComparing(candidate -> candidate.product().getNombre(),
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(limit)
                .map(candidate -> new CartSuggestion(
                        candidate.product(),
                        candidate.reason(),
                        candidate.source(),
                        candidate.score()))
                .toList();
    }

    private Optional<Candidate> score(
            Producto product,
            Set<UUID> favoriteIds,
            Set<UUID> detectedPreferenceIds,
            Map<UUID, Long> purchaseFrequency,
            Set<UUID> todayPatternIds,
            Map<UUID, Long> studentCartAffinities,
            Map<UUID, Long> buffetCartAffinities,
            Map<UUID, Producto> availableById) {
        UUID productId = product.getId();
        int score = 0;
        Map<String, Integer> sourceScores = new LinkedHashMap<>();

        Long studentAffinity = studentCartAffinities.get(productId);
        if (studentAffinity != null && studentAffinity > 0) {
            int affinityScore = Math.min(80, studentAffinity.intValue() * 20);
            score += affinityScore;
            sourceScores.put("STUDENT_CART_AFFINITY", affinityScore);
        }
        Long buffetAffinity = buffetCartAffinities.get(productId);
        if (buffetAffinity != null && buffetAffinity > 0) {
            int affinityScore = Math.min(60, buffetAffinity.intValue() * 15);
            score += affinityScore;
            sourceScores.put("BUFFET_CART_AFFINITY", affinityScore);
        }
        if (favoriteIds.contains(productId)) {
            score += 50;
            sourceScores.put("FAVORITE", 50);
        }
        if (detectedPreferenceIds.contains(productId)) {
            score += 40;
            sourceScores.put("DETECTED_PREFERENCE", 40);
        }
        Long frequency = purchaseFrequency.get(productId);
        if (frequency != null && frequency > 0) {
            int frequencyScore = Math.min(30, frequency.intValue() * 5);
            score += frequencyScore;
            sourceScores.put("PURCHASE_HISTORY", frequencyScore);
        }
        if (todayPatternIds.contains(productId)) {
            score += 20;
            sourceScores.put("DAY_PATTERN", 20);
        }

        if (score == 0 || !availableById.containsKey(productId)) {
            return Optional.empty();
        }

        String source = strongestSource(sourceScores);
        return Optional.of(new Candidate(product, source, reason(source), score));
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
}
