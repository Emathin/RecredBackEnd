package com.recred.backend.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Summary;
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
        List<UUID> processedIds = new ArrayList<>();
        List<Tutor> validTutors = findTutorsWithPaidPlanAndChildren();
        for (Tutor tutor : validTutors) {
            generateSummaryForTutor(tutor, request.start(), request.end());
            processedIds.add(tutor.getUsuario().getId());
        }
        return new GenerateParentSummaryResponse(processedIds);
    }

    private List<Tutor> findTutorsWithPaidPlanAndChildren() {
        return tutorRepository.findAll().stream()
                .filter(Tutor::canReceiveParentSummary)
                .toList();
    }

    private void generateSummaryForTutor(Tutor tutor, LocalDateTime start, LocalDateTime end) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode childrenNode = root.putObject("hijos");
        for (Alumno child : tutor.getHijos()) {
            processChildPurchases(child, start, end, childrenNode);
        }
        saveSummary(tutor, start, end, root);
    }

    private void processChildPurchases(Alumno child, LocalDateTime start, LocalDateTime end,
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

    private void addCategoryBreakdown(List<Purchase> purchases, BigDecimal total,
                                      ObjectNode childNode) {
        Map<String, BigDecimal> categories = new HashMap<>();
        purchases.forEach(p -> p.getItems().forEach((product, quantity) -> {
            String category = product.getCategoria() != null
                    ? product.getCategoria().getDescripcion() : "Sin Categoria";
            BigDecimal amount = product.getPrecio().multiply(BigDecimal.valueOf(quantity));
            categories.merge(category, amount, BigDecimal::add);
        }));
        ObjectNode categoriesNode = childNode.putObject("porCategoria");
        categories.forEach((category, amount) -> {
            BigDecimal divisor = total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : total;
            BigDecimal percentage = amount.divide(divisor, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            categoriesNode.put(category, percentage);
        });
    }

    private void addMostConsumedProduct(List<Purchase> purchases, ObjectNode childNode) {
        Map<Producto, Integer> products = new HashMap<>();
        purchases.forEach(p -> p.getItems().forEach(
                (product, quantity) -> products.merge(product, quantity, Integer::sum)));
        products.entrySet().stream()
                .max((e1, e2) -> {
                    int compare = e1.getValue().compareTo(e2.getValue());
                    if (compare != 0) {
                        return compare;
                    }
                    return e1.getKey().getPrecio().compareTo(e2.getKey().getPrecio());
                })
                .ifPresent(entry -> {
                    ObjectNode productNode = childNode.putObject("productoMasConsumido");
                    productNode.put("nombre", entry.getKey().getNombre());
                    productNode.put("precio", entry.getKey().getPrecio());
                    productNode.put("cantidad", entry.getValue());
                });
    }

    private void saveSummary(Tutor tutor, LocalDateTime start, LocalDateTime end, ObjectNode root) {
        String analysis = parentSummaryAnalyzer.analyze(root.toString());
        root.put("mensaje", analysis);
        Summary summary = new Summary();
        summary.setFechaDesde(start);
        summary.setFechaHasta(end);
        summary.setUsuario(tutor.getUsuario());
        summary.setResumen(root.toString());
        summaryRepository.save(summary);
    }
}
