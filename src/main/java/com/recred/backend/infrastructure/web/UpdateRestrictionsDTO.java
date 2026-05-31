package com.recred.backend.infrastructure.web;

import java.util.List;
import java.util.UUID;

public class UpdateRestrictionsDTO {
    private List<UUID> clasificationIDs;

    public List<UUID> getClasificationIDs() {
        return clasificationIDs;
    }

    public void setClasificationIDs(List<UUID> clasificationIDs) {
        this.clasificationIDs = clasificationIDs;
    }
}