package com.recred.backend.domain.services;

import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.springframework.web.multipart.MultipartFile;

public interface LoadStockAnalyzer {
    LoadStockPredictionDTO analyze(MultipartFile image);
}
