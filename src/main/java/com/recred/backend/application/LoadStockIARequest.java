package com.recred.backend.application;

public record LoadStockIARequest(
    byte[] fileBytes,
    String fileName,
    String contentType,
    long fileSize
) {}
