package com.recred.backend.application;

public record ParseBulkStockCommand(byte[] fileContent, String contentType) {
}
