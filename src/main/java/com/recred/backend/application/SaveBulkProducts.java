package com.recred.backend.application;

import java.util.stream.Collectors;

public class SaveBulkProducts {

    private final SaveProduct saveProduct;

    public SaveBulkProducts(SaveProduct saveProduct) {
        this.saveProduct = saveProduct;
    }

    public SaveBulkProductsResponse execute(SaveBulkProductsRequest request) {
        return new SaveBulkProductsResponse(request.products().stream()
                .map(saveProduct::execute)
                .collect(Collectors.toList()));
    }
}
