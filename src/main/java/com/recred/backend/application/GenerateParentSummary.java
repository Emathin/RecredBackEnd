package com.recred.backend.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class GenerateParentSummary {
    private final TutorRepository tutorRepository;
    private final PurchaseRepository purchaseRepository;
    private final SummaryRepository summaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateParentSummary(TutorRepository tutorRepository,
                                 PurchaseRepository purchaseRepository,
                                 SummaryRepository summaryRepository) {
        this.tutorRepository = tutorRepository;
        this.purchaseRepository = purchaseRepository;
        this.summaryRepository = summaryRepository;
    }

    public Summary execute(UUID usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        var tutor = tutorRepository.findByUsuarioId(usuarioId).orElseThrow();
        Set<Alumno> hijos = tutor.getHijos();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("periodoDesde", inicio.toString());
        root.put("periodoHasta", fin.toString());

        ObjectNode hijosNode = root.putObject("hijos");

        for (Alumno hijo : hijos) {
            List<Purchase> compras = purchaseRepository.findByAlumnoAndDateRange(hijo.getId(), inicio, fin);
            BigDecimal totalHijo = compras.stream().map(Purchase::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            ObjectNode hijoNode = hijosNode.putObject(hijo.getUsuario().getNombre());
            hijoNode.put("totalGastado", totalHijo);
// Categorias
Map<String, BigDecimal> categorias = new HashMap<>();
compras.forEach(c -> c.getItems().forEach((producto, cantidad) -> {
    String cat = producto.getCategoria() != null ?
            producto.getCategoria().getDescripcion() : "Sin Categoria";
    categorias.merge(cat, producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)),
            BigDecimal::add);
}));
ObjectNode catsNode = hijoNode.putObject("porCategoria");
categorias.forEach((cat, monto) -> {
    BigDecimal porcentaje = totalHijo.compareTo(BigDecimal.ZERO) == 0 ?
            BigDecimal.ZERO :
            monto.divide(totalHijo, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
    catsNode.put(cat, porcentaje + "%");
});

// Productos
Map<Producto, Integer> productos = new HashMap<>();
compras.forEach(c -> c.getItems().forEach((p, q) -> productos.merge(p, q, Integer::sum)));

var sortedProductos = productos.entrySet().stream()
        .sorted((e1, e2) -> e2.getKey().getPrecio()
                .compareTo(e1.getKey().getPrecio()))
        .limit(5)
        .toList();
            var prodsNode = hijoNode.putArray("productosMasConsumidos");
            sortedProductos.forEach(e -> {
                ObjectNode pNode = prodsNode.addObject();
                pNode.put("nombre", e.getKey().getNombre());
                pNode.put("precio", e.getKey().getPrecio());
                pNode.put("cantidad", e.getValue());
            });
        }

        Summary summary = new Summary();
        summary.setFechaDesde(inicio);
        summary.setFechaHasta(fin);
        summary.setUsuario(tutor.getUsuario());
        summary.setResumen(root.toString());
        
        return summaryRepository.save(summary);
    }
}
