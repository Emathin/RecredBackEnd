package com.recred.backend.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GenerateParentSummary {

    private final TutorRepository tutorRepository;
    private final PurchaseRepository purchaseRepository;
    private final SummaryRepository summaryRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final ParentSummaryAnalyzer parentSummaryAnalyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateParentSummary(TutorRepository tutorRepository,
                                 PurchaseRepository purchaseRepository,
                                 SummaryRepository summaryRepository,
                                 PresupuestoRepository presupuestoRepository,
                                 ParentSummaryAnalyzer parentSummaryAnalyzer) {
        this.tutorRepository = tutorRepository;
        this.purchaseRepository = purchaseRepository;
        this.summaryRepository = summaryRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.parentSummaryAnalyzer = parentSummaryAnalyzer;
    }

    public GenerateParentSummaryResponse execute(GenerateParentSummaryRequest request) {
        List<UUID> processedTutorUserIds = generateSummariesForPremiumTutors(
                request.start(),
                request.end()
        );

        return buildResponse(processedTutorUserIds);
    }

    private List<UUID> generateSummariesForPremiumTutors(
            LocalDateTime start,
            LocalDateTime end
    ) {
        List<UUID> processedTutorUserIds = new ArrayList<>();

        for (Tutor tutor : findPremiumTutorsWithChildren()) {
            saveSummaryForTutor(tutor, start, end);
            processedTutorUserIds.add(tutorUserId(tutor));
        }

        return processedTutorUserIds;
    }

    private GenerateParentSummaryResponse buildResponse(List<UUID> processedTutorUserIds) {
        return new GenerateParentSummaryResponse(processedTutorUserIds);
    }

    private List<Tutor> findPremiumTutorsWithChildren() {
        return tutorRepository.findAll().stream()
                .filter(this::tutorHasPremiumPlan)
                .filter(this::tutorHasChildren)
                .toList();
    }

    private boolean tutorHasPremiumPlan(Tutor tutor) {
        return TipoPlan.PREMIUM.equals(tutor.getUsuario().getPlan());
    }

    private boolean tutorHasChildren(Tutor tutor) {
        return !tutor.getHijos().isEmpty();
    }

    private UUID tutorUserId(Tutor tutor) {
        return tutor.getUsuario().getId();
    }

    private void saveSummaryForTutor(Tutor tutor, LocalDateTime start, LocalDateTime end) {
        ObjectNode summaryContent = summaryContentFor(tutor, start, end);
        addAnalyzerMessage(summaryContent);
        saveSummary(tutor, start, end, summaryContent);
    }

    private ObjectNode summaryContentFor(Tutor tutor, LocalDateTime start, LocalDateTime end) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode childrenNode = root.putObject("hijos");
        for (Alumno child : tutor.getHijos()) {
            addChildSummary(child, start, end, childrenNode);
        }
        return root;
    }

    private void addChildSummary(
            Alumno child,
            LocalDateTime start,
            LocalDateTime end,
            ObjectNode childrenNode) {
        List<Purchase> purchases = purchaseRepository.findByAlumnoAndDateRange(
                child.getId(), start, end);
        BigDecimal total = calculateTotal(purchases);
        ObjectNode childNode = childrenNode.putObject(child.getUsuario().getNombre());
        childNode.put("totalGastado", total);
        addBudgetLimit(child, childNode);
        addCategoryBreakdown(purchases, total, childNode);
        addMostConsumedProduct(purchases, childNode);
    }

    private BigDecimal calculateTotal(List<Purchase> purchases) {
        return purchases.stream()
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addBudgetLimit(Alumno child, ObjectNode childNode) {
        presupuestoRepository.findActivoByAlumnoId(child.getId())
                .ifPresent(budget -> childNode.put("LimiteGasto", budget.getMontoLimite()));
    }

    private void addCategoryBreakdown(
            List<Purchase> purchases,
            BigDecimal total,
            ObjectNode childNode) {
        ObjectNode categoriesNode = childNode.putObject("porCategoria");
        spendingByCategory(purchases).forEach((category, amount) ->
                categoriesNode.put(category, percentageOfTotal(amount, total)));
    }

    private Map<String, BigDecimal> spendingByCategory(List<Purchase> purchases) {
        Map<String, BigDecimal> categories = new HashMap<>();
        purchases.forEach(purchase -> purchase.getItems().forEach((product, quantity) -> {
            String category = product.getCategoria() != null
                    ? product.getCategoria().getDescripcion() : "Sin Categoria";
            BigDecimal amount = product.getPrecio().multiply(BigDecimal.valueOf(quantity));
            categories.merge(category, amount, BigDecimal::add);
        }));
        return categories;
    }

    private BigDecimal percentageOfTotal(BigDecimal amount, BigDecimal total) {
        BigDecimal divisor = total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : total;
        return amount.divide(divisor, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
    }

    private void addMostConsumedProduct(List<Purchase> purchases, ObjectNode childNode) {
        consumedQuantitiesByProduct(purchases).entrySet().stream()
                .max(this::compareConsumedProducts)
                .ifPresent(entry -> {
                    ObjectNode productNode = childNode.putObject("productoMasConsumido");
                    productNode.put("nombre", entry.getKey().getNombre());
                    productNode.put("precio", entry.getKey().getPrecio());
                    productNode.put("cantidad", entry.getValue());
                });
    }

    private Map<Producto, Integer> consumedQuantitiesByProduct(List<Purchase> purchases) {
        Map<Producto, Integer> products = new HashMap<>();
        purchases.forEach(purchase -> purchase.getItems().forEach(
                (product, quantity) -> products.merge(product, quantity, Integer::sum)));
        return products;
    }

    private int compareConsumedProducts(
            Map.Entry<Producto, Integer> first,
            Map.Entry<Producto, Integer> second) {
        int quantityComparison = first.getValue().compareTo(second.getValue());
        if (quantityComparison != 0) {
            return quantityComparison;
        }
        return first.getKey().getPrecio().compareTo(second.getKey().getPrecio());
    }

    private void addAnalyzerMessage(ObjectNode summaryContent) {
        String analysis = parentSummaryAnalyzer.analyze(summaryContent.toString());
        summaryContent.put("mensaje", analysis);
    }

    private void saveSummary(
            Tutor tutor,
            LocalDateTime start,
            LocalDateTime end,
            ObjectNode summaryContent) {
        Summary summary = new Summary();
        summary.setFechaDesde(start);
        summary.setFechaHasta(end);
        summary.setUsuario(tutor.getUsuario());
        summary.setResumen(summaryContent.toString());
        summaryRepository.save(summary);
    }
}
