package com.recred.backend.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;}

    public String uploadImage(byte[] fileBytes, String originalFilename) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    fileBytes,
                    ObjectUtils.asMap("resource_type", "image")
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Error al subir imagen a Cloudinary", e);
        }
    }
}
