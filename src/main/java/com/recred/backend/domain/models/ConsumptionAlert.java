package com.recred.backend.domain.models;

public record ConsumptionAlert(
        ConsumptionAlertCandidate candidate,
        ConsumptionAlertPattern pattern,
        ConsumptionAlertMessages messages
) {
}
