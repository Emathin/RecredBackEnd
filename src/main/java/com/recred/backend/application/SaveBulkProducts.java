package com.recred.backend.application;

import java.util.List;
import java.util.stream.Collectors;

public class SaveBulkProducts {

    private final SaveProduct saveProduct;

    public SaveBulkProducts(SaveProduct saveProduct) {
        this.saveProduct = saveProduct;
    }

    public List<ProductResponse> execute(List<SaveProductCommand> commands) {
        return commands.stream()
                .map(saveProduct::execute)
                .map(ProductResponse::fromDomain)
                .collect(Collectors.toList());
    }
}
