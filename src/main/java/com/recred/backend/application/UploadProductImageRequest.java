package com.recred.backend.application;

import java.util.UUID;

public record UploadProductImageRequest(
        UUID buffetId,
        UUID productoId,
        byte[] imageBytes,
        String originalFilename
) {}
