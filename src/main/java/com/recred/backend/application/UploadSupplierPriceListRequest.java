package com.recred.backend.application;

import com.recred.backend.domain.exceptions.FileValidationException;
import java.util.UUID;

public record UploadSupplierPriceListRequest(
        UUID kioskOwnerId,
        UUID supplierId,
        byte[] fileContent,
        String originalFilename,
        String contentType
) {
    private static final int MAX_FILE_SIZE_IN_BYTES = 5 * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String TEXT_CONTENT_TYPE = "text/plain";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String CSV_EXTENSION = ".csv";

    public void validate() {
        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }

        if (supplierId == null) {
            throw new IllegalArgumentException("supplierId is required");
        }

        if (fileContent == null || fileContent.length == 0) {
            throw new FileValidationException("File cannot be empty");
        }

        if (fileContent.length > MAX_FILE_SIZE_IN_BYTES) {
            throw new FileValidationException(
                    "File exceeds the maximum allowed size (5MB)");
        }

        if (!isSupportedFile()) {
            throw new FileValidationException(
                    "Unsupported file format. Only PDF and CSV files are allowed");
        }
    }

    String resolvedMimeType() {
        return isPdf() ? PDF_CONTENT_TYPE : CSV_CONTENT_TYPE;
    }

    private boolean isSupportedFile() {
        return isPdf() || isCsv();
    }

    private boolean isPdf() {
        return PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || PDF_EXTENSION.equals(extension());
    }

    private boolean isCsv() {
        return CSV_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || TEXT_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || CSV_EXTENSION.equals(extension());
    }

    private String extension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();
    }
}
