package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.CompraRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.CompraEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CompraRepositoryImpl implements CompraRepository {

    private final SpringDataCompraRepository springDataRepository;

    public CompraRepositoryImpl(SpringDataCompraRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<PreferenceAIRepository.CompraResumen> findLastPurchasesByAlumno(
            UUID alumnoId, int limit) {
        
        List<CompraEntity> compras = 
                springDataRepository.findLastByAlumno(alumnoId, PageRequest.of(0, limit));
        
        return compras.stream()
                .flatMap(c -> c.getDetalles().stream())
                .map(d -> {
                    String catName = "Sin Categoria";
                    if (d.getProductoEntity().getCategoria() != null) {
                        catName = d.getProductoEntity().getCategoria().getDescripcion();
                    }
                    return new PreferenceAIRepository.CompraResumen(
                        d.getProductoEntity().getNombre(),
                        catName,
                        d.getCompra().getFechaHora()
                    );
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}
