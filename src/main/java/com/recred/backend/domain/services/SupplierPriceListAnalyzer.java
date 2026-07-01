package com.recred.backend.domain.services;

import com.recred.backend.application.RawSupplierPriceItem;
import java.util.List;

public interface SupplierPriceListAnalyzer {
    List<RawSupplierPriceItem> analyze(byte[] fileContent, String contentType);
}
