package com.recred.backend.application;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record UploadProductImageRequest(
        UUID buffetId,
        UUID productoId,
        MultipartFile imagen
) {}
