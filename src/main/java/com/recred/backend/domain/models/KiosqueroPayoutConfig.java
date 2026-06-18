package com.recred.backend.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class KiosqueroPayoutConfig {
    private UUID kiosqueroId;
    private String destinationCvu;
    private String destinationCuit;
    private String accountHolderName;
    private int cantidadIntervalo;
    private IntervalUnit unidadIntervalo;
    private PayoutScheduleStatus estado;
    private LocalDate proximaEjecucion;
    private LocalDate ultimaEjecucion;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public void calculateNextPayout(LocalDate baseDate) {
        if (this.unidadIntervalo == null || this.cantidadIntervalo <= 0) {
            return;
        }
        
        ChronoUnit unit = ChronoUnit.valueOf(this.unidadIntervalo.name());
        this.proximaEjecucion = baseDate.plus(this.cantidadIntervalo, unit);
    }

    public boolean isDue(LocalDate date) {
        if (this.estado != PayoutScheduleStatus.ACTIVE) {
            return false;
        }
        if (this.proximaEjecucion == null) {
            return false;
        }
        return !this.proximaEjecucion.isAfter(date);
    }

    public UUID getKiosqueroId() {
        return kiosqueroId;
    }

    public void setKiosqueroId(UUID kiosqueroId) {
        this.kiosqueroId = kiosqueroId;
    }

    public String getDestinationCvu() {
        return destinationCvu;
    }

    public void setDestinationCvu(String destinationCvu) {
        this.destinationCvu = destinationCvu;
    }

    public String getDestinationCuit() {
        return destinationCuit;
    }

    public void setDestinationCuit(String destinationCuit) {
        this.destinationCuit = destinationCuit;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public int getCantidadIntervalo() {
        return cantidadIntervalo;
    }

    public void setCantidadIntervalo(int cantidadIntervalo) {
        this.cantidadIntervalo = cantidadIntervalo;
    }

    public IntervalUnit getUnidadIntervalo() {
        return unidadIntervalo;
    }

    public void setUnidadIntervalo(IntervalUnit unidadIntervalo) {
        this.unidadIntervalo = unidadIntervalo;
    }

    public PayoutScheduleStatus getEstado() {
        return estado;
    }

    public void setEstado(PayoutScheduleStatus estado) {
        this.estado = estado;
    }

    public LocalDate getProximaEjecucion() {
        return proximaEjecucion;
    }

    public void setProximaEjecucion(LocalDate proximaEjecucion) {
        this.proximaEjecucion = proximaEjecucion;
    }

    public LocalDate getUltimaEjecucion() {
        return ultimaEjecucion;
    }

    public void setUltimaEjecucion(LocalDate ultimaEjecucion) {
        this.ultimaEjecucion = ultimaEjecucion;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
