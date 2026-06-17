package com.recred.backend.domain.services;

import com.recred.backend.application.RawBulkProduct;
import java.util.List;

public interface BulkStockAnalyzer {
    List<RawBulkProduct> analyze(byte[] fileContent, String contentType);
}
