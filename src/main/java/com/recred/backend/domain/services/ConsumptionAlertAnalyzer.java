package com.recred.backend.domain.services;

import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import java.util.Optional;

public interface ConsumptionAlertAnalyzer {
    Optional<ConsumptionAlertMessages> analyze(ConsumptionAlertAnalysisRequest request);
}
