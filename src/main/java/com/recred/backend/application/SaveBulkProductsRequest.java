package com.recred.backend.application;

import java.util.List;

public record SaveBulkProductsRequest(List<SaveProductRequest> products) {
}
