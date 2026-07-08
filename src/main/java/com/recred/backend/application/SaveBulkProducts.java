package com.recred.backend.application;

import java.util.List;

public class SaveBulkProducts {

    private final SaveProduct saveProduct;

    public SaveBulkProducts(SaveProduct saveProduct) {
        this.saveProduct = saveProduct;
    }

    public SaveBulkProductsResponse execute(SaveBulkProductsRequest bulkProductsRequest) {
        return new SaveBulkProductsResponse(savedProductsFrom(bulkProductsRequest));
    }

    private List<ProductResponse> savedProductsFrom(
            SaveBulkProductsRequest bulkProductsRequest
    ) {
        return bulkProductsRequest.products().stream()
                .map(saveProduct::execute)
                .toList();
    }
}
