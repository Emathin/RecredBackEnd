package com.recred.backend.application;

import org.springframework.web.multipart.MultipartFile;

public record ParseBulkStockCommand(MultipartFile file) {
}
