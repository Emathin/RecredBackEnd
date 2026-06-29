package com.recred.backend.application;

import com.recred.backend.domain.services.LoadStockAnalyzer;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;

public class LoadStockIA {
    private final LoadStockAnalyzer loadStockAnalyzer;
    private final CloudinaryService cloudinaryService;

    public LoadStockIA(LoadStockAnalyzer loadStockAnalyzer, CloudinaryService cloudinaryService) {
        this.loadStockAnalyzer = loadStockAnalyzer;
        this.cloudinaryService = cloudinaryService;
    }

    public LoadStockIAResponse execute(LoadStockIARequest request) {
        String imageUrl = cloudinaryService.uploadImage(request.fileBytes(), request.fileName());
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
