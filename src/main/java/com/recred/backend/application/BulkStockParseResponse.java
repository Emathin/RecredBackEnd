package com.recred.backend.application;

import com.recred.backend.infrastructure.web.SaveProductDTO;
import java.util.List;

public record BulkStockParseResponse(List<SaveProductDTO> products) {
}
