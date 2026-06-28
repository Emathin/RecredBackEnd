package com.recred.backend.domain.services;

public interface StorageService {
    String uploadFile(byte[] fileContent, String originalFilename, String contentType);
}
