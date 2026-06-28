package com.recred.backend.application;

import java.util.UUID;

public record GetActiveSchoolCreditRequest(UUID studentId) {
    public void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId cannot be null");
        }
    }
}
