package com.recred.backend.domain.services;

import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;

public interface LoadStockAnalyzer {
    LoadStockIAResponse analyze(LoadStockIARequest request);
}
