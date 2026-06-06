package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PresupuestoAlumno {
    private UUID id;
    private UUID alumnoId;
    private UUID creadoPorUsuarioId;
    private BigDecimal montoLimiteGeneral;
    private PeriodoPresupuesto periodo;
    private LocalDate fechaInicio;
    private boolean activo;
    private List<ReglaPresupuestoCategoria> reglasCategoria;

    public PresupuestoAlumno() {
        this.reglasCategoria = new ArrayList<>();
        this.activo = true;
    }

    public PresupuestoAlumno(UUID id, UUID alumnoId, UUID creadoPorUsuarioId,
            BigDecimal montoLimiteGeneral, PeriodoPresupuesto periodo, LocalDate fechaInicio,
            boolean activo, List<ReglaPresupuestoCategoria> reglasCategoria) {
        this.id = id;
        this.alumnoId = alumnoId;
        this.creadoPorUsuarioId = creadoPorUsuarioId;
        this.montoLimiteGeneral = montoLimiteGeneral;
        this.periodo = periodo;
        this.fechaInicio = fechaInicio;
        this.activo = activo;
        this.reglasCategoria = reglasCategoria != null ? reglasCategoria : new ArrayList<>();
    }

    public boolean esVigente(LocalDate fechaActual) {
        return activo && !fechaActual.isBefore(fechaInicio);
    }

    public void validarPorcentajes() {
        if (this.reglasCategoria == null || this.reglasCategoria.isEmpty()) {
            return;
        }

        BigDecimal sumaPorcentajes = this.reglasCategoria.stream()
                .filter(ReglaPresupuestoCategoria::isActivo)
                .map(r -> r.getPorcentajeLimite() != null ? r.getPorcentajeLimite() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPorcentajes.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("La suma de los porcentajes " +
                    "no puede superar el 100%. Total enviado: " + sumaPorcentajes + "%");
        }
    }

    public void recalcularMontosReglas() {
        if (this.reglasCategoria == null || this.reglasCategoria.isEmpty() ||
                this.montoLimiteGeneral == null) {
            return;
        }

        for (ReglaPresupuestoCategoria regla : this.reglasCategoria) {
            if (regla.getPorcentajeLimite() != null && regla.isActivo()) {
                BigDecimal montoCalculado = this.montoLimiteGeneral
                        .multiply(regla.getPorcentajeLimite())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                regla.setMontoLimiteCalculado(montoCalculado);
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(UUID alumnoId) {
        this.alumnoId = alumnoId;
    }

    public BigDecimal getMontoLimiteGeneral() {
        return montoLimiteGeneral;
    }

    public void setMontoLimiteGeneral(BigDecimal montoLimiteGeneral) {
        this.montoLimiteGeneral = montoLimiteGeneral;
    }

    public PeriodoPresupuesto getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoPresupuesto periodo) {
        this.periodo = periodo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public List<ReglaPresupuestoCategoria> getReglasCategoria() {
        return reglasCategoria;
    }

    public void setReglasCategoria(List<ReglaPresupuestoCategoria> reglasCategoria) {
        this.reglasCategoria = reglasCategoria;
    }

    public UUID getCreadoPorUsuarioId() {
        return creadoPorUsuarioId;
    }

    public void setCreadoPorUsuarioId(UUID creadoPorUsuarioId) {
        this.creadoPorUsuarioId = creadoPorUsuarioId;
    }
}