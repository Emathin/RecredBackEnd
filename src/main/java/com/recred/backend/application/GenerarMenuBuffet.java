package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenerarMenuBuffet {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final PurchaseRepository purchaseRepository;
    private final CalculadorPeriodoPresupuesto calculadorPeriodo;

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Argentina/Buenos_Aires");

    public GenerarMenuBuffet(AlumnoRepository alumnoRepository,
                             ProductRepository productRepository,
                             PresupuestoRepository presupuestoRepository,
                             PurchaseRepository purchaseRepository,
                             CalculadorPeriodoPresupuesto calculadorPeriodo) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.purchaseRepository = purchaseRepository;
        this.calculadorPeriodo = calculadorPeriodo;
    }

    @Transactional(readOnly = true)
    public List<MenuProductoDTO> execute(UUID alumnoId, LocalDateTime momentoConsulta) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        List<Producto> catalogo = productRepository.findAllActive();

        LocalDateTime momentoLocal = ZonedDateTime.of(momentoConsulta, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL).toLocalDateTime();

        BigDecimal presupuestoGeneralDisponible = null;
        Map<UUID, BigDecimal> presupuestoCategoriaDisponible = new HashMap<>();

        Optional<PresupuestoAlumno> presupuestoOpt = presupuestoRepository.findActivoByAlumnoId
                (alumnoId);
        if (presupuestoOpt.isPresent() && presupuestoOpt.get().esVigente(momentoConsulta.
                toLocalDate())) {
            PresupuestoAlumno p = presupuestoOpt.get();
            RangoFechas rango = calculadorPeriodo.calcularRango(p.getPeriodo(), momentoConsulta);

            BigDecimal gastadoHistorico = purchaseRepository.sumarGastosPorAlumnoEnRango
                    (alumnoId, rango.inicio(), rango.fin());
            presupuestoGeneralDisponible = p.getMontoLimiteGeneral().subtract(gastadoHistorico);

            if (p.getReglasCategoria() != null) {
                for (ReglaPresupuestoCategoria regla : p.getReglasCategoria()) {
                    if (regla.isActivo()) {
                        UUID catId = regla.getCategoria().getId();
                        BigDecimal gastadoCat = purchaseRepository.sumarGastosPorCategoriaEnRango
                                (alumnoId, catId, rango.inicio(), rango.fin());
                        presupuestoCategoriaDisponible.put(catId, regla.getMontoLimiteCalculado().
                                subtract(gastadoCat));
                    }
                }
            }
        }

        final BigDecimal presupuestoFinalParaLambda = presupuestoGeneralDisponible;

        return catalogo.stream().map(producto -> {

            boolean bloqueoManual = alumno.getProductosBloqueados() != null &&
                    alumno.getProductosBloqueados().stream().anyMatch(pb ->
                            pb.getId().equals(producto.getId()));
            if (bloqueoManual) {
                return MenuProductoDTO.bloqueado(producto.getId(), producto.getNombre(),
                        producto.getDescripcion(), producto.getPrecio(),
                        "Bloqueado por el tutor");
            }

            if (alumno.getRestriccionesSalud() != null) {
                String saludConflictos = alumno.getRestriccionesSalud().stream()
                        .filter(restriccion -> producto.getClasificaciones().
                                stream().anyMatch(c -> c.getId().equals(restriccion.getId())))
                        .map(ClasificacionSalud::getDescripcion)
                        .collect(Collectors.joining(", "));

                if (!saludConflictos.isEmpty()) {
                    return MenuProductoDTO.bloqueado(producto.getId(), producto.getNombre(),
                            producto.getDescripcion(), producto.getPrecio(), "Contiene: " +
                                    saludConflictos);
                }
            }

            if (alumno.getRestriccionesHorarias() != null) {
                boolean fueraDeHorario = alumno.getRestriccionesHorarias().stream()
                        .anyMatch(rh -> rh.bloquea(producto, momentoLocal));
                if (fueraDeHorario) {
                    return MenuProductoDTO.bloqueado(producto.getId(), producto.getNombre(),
                            producto.getDescripcion(), producto.getPrecio(),
                            "No permitido en este horario");
                }
            }

            if (presupuestoFinalParaLambda != null) {
                if (producto.getPrecio().compareTo(presupuestoFinalParaLambda) > 0) {
                    return MenuProductoDTO.bloqueado(producto.getId(), producto.getNombre(),
                            producto.getDescripcion(), producto.getPrecio(),
                            "Supera el límite de gasto");
                }

                if (producto.getCategoria() != null && presupuestoCategoriaDisponible
                        .containsKey(producto.getCategoria().getId())) {
                    BigDecimal dispCat = presupuestoCategoriaDisponible.get(producto
                            .getCategoria().getId());
                    if (producto.getPrecio().compareTo(dispCat) > 0) {
                        return MenuProductoDTO.bloqueado(producto.getId(), producto.getNombre(),
                                producto.getDescripcion(), producto.getPrecio(),
                                "Supera límite de su categoría");
                    }
                }
            }

            return MenuProductoDTO.permitido(producto.getId(), producto.getNombre(),
                    producto.getDescripcion(), producto.getPrecio());

        }).toList();
    }
}