package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ValidadorPresupuestoService {

    private final PresupuestoRepository presupuestoRepository;
    private final PurchaseRepository purchaseRepository;
    private final CalculadorPeriodoPresupuesto calculadorPeriodo;

    public ValidadorPresupuestoService(PresupuestoRepository presupuestoRepository,
                                       PurchaseRepository purchaseRepository,
                                       CalculadorPeriodoPresupuesto calculadorPeriodo) {
        this.presupuestoRepository = presupuestoRepository;
        this.purchaseRepository = purchaseRepository;
        this.calculadorPeriodo = calculadorPeriodo;
    }

    public void validarPresupuesto(UUID alumnoId,
                                   BigDecimal montoTotalCarrito,
                                   Map<UUID, BigDecimal> montosPorCategoria,
                                   LocalDateTime momentoCompra) {

        Optional<PresupuestoAlumno> presupuestoOpt =
                presupuestoRepository.findActivoByAlumnoId(alumnoId);
        if (presupuestoOpt.isEmpty()) {
            return;
        }

        PresupuestoAlumno presupuesto = presupuestoOpt.get();

        if (!presupuesto.esVigente(momentoCompra.toLocalDate())) {
            return;
        }

        RangoFechas rango = calculadorPeriodo.calcularRango(
                presupuesto.getPeriodo(), momentoCompra);

        BigDecimal gastadoHistorico = purchaseRepository.sumarGastosPorAlumnoEnRango(
                alumnoId, rango.inicio(), rango.fin()
        );

        BigDecimal totalProyectado = gastadoHistorico.add(montoTotalCarrito);

        if (totalProyectado.compareTo(presupuesto.getMontoLimiteGeneral()) > 0) {
            BigDecimal disponible = presupuesto.getMontoLimiteGeneral().subtract(gastadoHistorico);
            throw new PresupuestoExcedidoException(
                    String.format("Presupuesto %s excedido. Disponible: $%s",
                            presupuesto.getPeriodo().name().toLowerCase(),
                            disponible.max(BigDecimal.ZERO))
            );
        }

        if (presupuesto.getReglasCategoria() != null && !presupuesto.
                getReglasCategoria().isEmpty()) {
            for (ReglaPresupuestoCategoria regla : presupuesto.getReglasCategoria()) {
                UUID catId = regla.getCategoria().getId();

                BigDecimal gastoCategoriaEnCarrito = montosPorCategoria.
                        getOrDefault(catId, BigDecimal.ZERO);

                if (gastoCategoriaEnCarrito.compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal gastadoHistoricoCat = purchaseRepository.
                            sumarGastosPorCategoriaEnRango(
                            alumnoId, catId, rango.inicio(), rango.fin()
                    );

                    BigDecimal proyectadoCat = gastadoHistoricoCat.add(gastoCategoriaEnCarrito);

                    if (proyectadoCat.compareTo(regla.getMontoLimiteCalculado()) > 0) {
                        BigDecimal disponibleCat = regla.getMontoLimiteCalculado().
                                subtract(gastadoHistoricoCat);
                        throw new PresupuestoExcedidoException(
                                String.format("Límite superado para una categoría restringida. " +
                                                "Disponible en categoría: $%s",
                                        disponibleCat.max(BigDecimal.ZERO))
                        );
                    }
                }
            }
        }
    }
}