package com.recred.backend.application;

import java.util.List;
import java.util.UUID;

public record ManageRestrictionsRequest(
        UUID studentId,
        List<UUID> newClassificationIds
) {}
