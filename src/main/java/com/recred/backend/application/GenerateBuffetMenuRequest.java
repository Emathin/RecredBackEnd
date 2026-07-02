package com.recred.backend.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenerateBuffetMenuRequest(
        UUID alumnoId,
        UUID buffetId,
        LocalDateTime momentoConsulta
) {
}
