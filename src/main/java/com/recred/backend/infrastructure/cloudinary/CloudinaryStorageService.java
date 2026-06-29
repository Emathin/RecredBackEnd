package com.recred.backend.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recred.backend.domain.services.StorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadFile(byte[] fileContent, String originalFilename, String contentType) {
        try {
            String cleanName = originalFilename != null 
                    ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") 
                    : "file_" + System.currentTimeMillis();

            Map<?, ?> result = cloudinary.uploader().upload(
                    fileContent,
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "public_id", cleanName
                    )
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary", e);
        }
    }
}
