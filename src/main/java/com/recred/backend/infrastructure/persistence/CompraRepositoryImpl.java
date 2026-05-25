package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.repositories.CompraRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CompraRepositoryImpl implements CompraRepository {

    private final SpringDataCompraRepository
            springDataRepository;

    public CompraRepositoryImpl(SpringDataCompraRepository
                                        springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public BigDecimal sumarGastosPorAlumnoEnRango
            (UUID alumnoId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.
                sumarGastosGeneralesEnRango(alumnoId,
                        EstadoCompra.ENTREGADO, inicio, fin);
    }

    @Override
    public BigDecimal sumarGastosPorCategoriaEnRango
            (UUID alumnoId, UUID categoriaId, LocalDateTime inicio,
             LocalDateTime fin) {
        return springDataRepository.sumarGastosPorCategoriaEnRango(
                alumnoId, EstadoCompra.ENTREGADO, categoriaId, inicio, fin);
    }
}