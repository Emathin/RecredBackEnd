package com.recred.backend.application;

import java.util.UUID;

public record GetProductByIdRequest(UUID id, UUID buffetId) {
}
