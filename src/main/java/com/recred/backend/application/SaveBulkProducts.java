package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;

import java.util.List;
import java.util.stream.Collectors;

public class SaveBulkProducts {

    private final SaveProduct saveProduct;

    public SaveBulkProducts(SaveProduct saveProduct) {
        this.saveProduct = saveProduct;
    }

    public List<Producto> execute(List<SaveProductCommand> commands) {
        return commands.stream()
                .map(saveProduct::execute)
                .collect(Collectors.toList());
    }
}
