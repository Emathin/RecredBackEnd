package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SupplierDetailsResponse(
        UUID id,
        UUID kioskOwnerId,
        String name,
        String phone,
        String email,
        String visitDays,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<UploadSupplierPriceListResponse> priceLists
) {
    static SupplierDetailsResponse fromDomain(Proveedor supplier) {
        return new SupplierDetailsResponse(
                supplier.getId(),
                supplier.getKiosqueroId(),
                supplier.getNombre(),
                supplier.getTelefono(),
                supplier.getEmail(),
                supplier.getDiasVisita(),
                supplier.getNotas(),
                supplier.getCreadoEn(),
                supplier.getActualizadoEn(),
                supplier.priceListsOrEmpty()
                        .stream()
                        .map(UploadSupplierPriceListResponse::fromDomain)
                        .toList()
        );
    }
}
