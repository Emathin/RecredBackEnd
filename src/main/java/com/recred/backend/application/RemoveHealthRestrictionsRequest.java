package com.recred.backend.application;

import java.util.UUID;

public record RemoveHealthRestrictionsRequest(
        UUID alumnoId,
        UUID clasificacionId
) {}
