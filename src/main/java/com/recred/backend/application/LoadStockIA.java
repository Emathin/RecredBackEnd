package com.recred.backend.application;

import com.recred.backend.domain.services.LoadStockAnalyzer;
import com.recred.backend.domain.services.StorageService;

public class LoadStockIA {
    private final LoadStockAnalyzer loadStockAnalyzer;
    private final StorageService storageService;

    public LoadStockIA(LoadStockAnalyzer loadStockAnalyzer, StorageService storageService) {
        this.loadStockAnalyzer = loadStockAnalyzer;
        this.storageService = storageService;
    }

    public LoadStockIAResponse execute(LoadStockIARequest request) {
        String imageUrl = storageService.uploadFile(request.fileBytes(),
                request.fileName(), request.contentType());
        var prediction = loadStockAnalyzer.analyze(request);
        
        return new LoadStockIAResponse(
                prediction.name(),
                prediction.description(),
                prediction.weight(),
                prediction.containsSugar(),
                prediction.containsPeanuts(),
                prediction.containsLactose(),
                prediction.containsGluten(),
                imageUrl
        );
    }
}
