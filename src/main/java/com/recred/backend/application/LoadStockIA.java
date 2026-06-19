package com.recred.backend.application;

import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.springframework.web.multipart.MultipartFile;

public class LoadStockIA {
    private final LoadStockIAService loadStockIAService;
    private final CloudinaryService cloudinaryService;

    public LoadStockIA(LoadStockIAService loadStockIAService, CloudinaryService cloudinaryService) {
        this.loadStockIAService = loadStockIAService;
        this.cloudinaryService = cloudinaryService;
    }


    public LoadStockPredictionDTO procesarImagen(MultipartFile image) {
        String urlImagen = cloudinaryService.uploadImage(image);
        LoadStockPredictionDTO prediction = loadStockIAService.analyze(image);
        prediction.setUrlImagen(urlImagen);
        return prediction;
    }

}
