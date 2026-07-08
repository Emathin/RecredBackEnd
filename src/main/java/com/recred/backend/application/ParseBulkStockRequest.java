package com.recred.backend.application;

public record ParseBulkStockRequest(byte[] fileContent, String contentType) {
}
