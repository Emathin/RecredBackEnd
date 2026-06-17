package com.recred.backend.domain.services;

import com.recred.backend.application.RawBulkProduct;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BulkStockAnalyzer {
    List<RawBulkProduct> analyze(MultipartFile file);
}
