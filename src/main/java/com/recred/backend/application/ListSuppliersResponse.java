package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import java.util.List;

public record ListSuppliersResponse(List<SupplierDetailsResponse> suppliers) {
    static ListSuppliersResponse fromDomain(List<Proveedor> suppliers) {
        return new ListSuppliersResponse(
                suppliers.stream()
                        .map(SupplierDetailsResponse::fromDomain)
                        .toList()
        );
    }
}
