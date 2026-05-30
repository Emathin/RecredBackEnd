package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseResume;

import java.util.List;
import java.util.Optional;

public interface PurchaseAnalyzer {

    Optional<PurchasePatternAnalysis> analyze(List<PurchaseResume> purchases);
}
