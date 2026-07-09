package com.recred.backend.unit.infrastructure.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.recred.backend.infrastructure.cloudinary.CloudinaryStorageService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryStorageServiceTest {

    @Test
    void uploadFileShouldSanitizeNameAndReturnSecureUrl() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        byte[] content = "pdf".getBytes();
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(same(content), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.test/lista.pdf"));

        String url = new CloudinaryStorageService(cloudinary)
                .uploadFile(content, "lista precios (julio).pdf", "application/pdf");

        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(same(content), optionsCaptor.capture());
        assertEquals("https://cdn.test/lista.pdf", url);
        assertEquals("raw", optionsCaptor.getValue().get("resource_type"));
        assertEquals("lista_precios__julio_.pdf", optionsCaptor.getValue().get("public_id"));
    }

    @Test
    void uploadFileShouldUseFallbackNameWhenFilenameIsNull() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        byte[] content = "pdf".getBytes();
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(same(content), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.test/file.pdf"));

        String url = new CloudinaryStorageService(cloudinary)
                .uploadFile(content, null, "application/pdf");

        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(same(content), optionsCaptor.capture());
        assertEquals("https://cdn.test/file.pdf", url);
        assertEquals(true, String.valueOf(optionsCaptor.getValue().get("public_id"))
                .startsWith("file_"));
    }

    @Test
    void uploadFileShouldWrapIoException() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        byte[] content = "pdf".getBytes();
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(same(content), any(Map.class)))
                .thenThrow(new IOException("cloudinary down"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> new CloudinaryStorageService(cloudinary)
                        .uploadFile(content, "lista.pdf", "application/pdf")
        );

        assertEquals("Error al subir archivo a Cloudinary", exception.getMessage());
    }
}
