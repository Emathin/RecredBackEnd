package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.domain.models.Proveedor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        UUID kiosqueroId,
        String nombre,
        String telefono,
        String email,
        String diasVisita,
        String notas,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        List<ListaPrecioProveedorResponse> listasPrecios
) {
    public static SupplierResponse fromDomain(Proveedor domain) {
        if (domain == null) {
            return null;
        }
        return new SupplierResponse(
                domain.getId(),
                domain.getKiosqueroId(),
                domain.getNombre(),
                domain.getTelefono(),
                domain.getEmail(),
                domain.getDiasVisita(),
                domain.getNotas(),
                domain.getCreadoEn(),
                domain.getActualizadoEn(),
                domain.getListasPrecios() != null ? domain.getListasPrecios().stream()
                        .map(ListaPrecioProveedorResponse::fromDomain)
                        .toList() : List.of()
        );
    }

    public static SupplierResponse fromApplication(SupplierDetailsResponse response) {
        if (response == null) {
            return null;
        }
        return new SupplierResponse(
                response.id(),
                response.kioskOwnerId(),
                response.name(),
                response.phone(),
                response.email(),
                response.visitDays(),
                response.notes(),
                response.createdAt(),
                response.updatedAt(),
                response.priceLists() != null ? response.priceLists().stream()
                        .map(ListaPrecioProveedorResponse::fromApplication)
                        .toList() : List.of()
        );
    }
}
