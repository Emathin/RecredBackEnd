package com.recred.backend.application;

import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.springframework.web.multipart.MultipartFile;

public class LoadStockIA {
    private final LoadStockIAService loadStockIAService;

    public LoadStockIA(LoadStockIAService loadStockIAService) {
        this.loadStockIAService = loadStockIAService;
    }


    public LoadStockPredictionDTO procesarImagen(MultipartFile image) {
        return loadStockIAService.analyze(image);
    }

}
