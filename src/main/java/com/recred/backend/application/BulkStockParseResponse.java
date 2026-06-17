package com.recred.backend.application;

import java.util.List;

public record BulkStockParseResponse(List<BulkProductData> products) {
}
