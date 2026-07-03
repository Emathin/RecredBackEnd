package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;

import java.util.List;

public record TutorInvitationImportResponse(
        int totalRows,
        int createdInvitations,
        int resentInvitations,
        int alreadyAssociated,
        List<ErrorItem> errors
) {

    public static TutorInvitationImportResponse fromResult(
            ImportTutorInvitationsFromCsv.TutorInvitationImportResult result) {
        return new TutorInvitationImportResponse(
                result.totalRows(),
                result.createdInvitations(),
                result.resentInvitations(),
                result.alreadyAssociated(),
                result.errors().stream()
                        .map(error -> new ErrorItem(
                                error.row(),
                                error.email(),
                                error.message()))
                        .toList()
        );
    }

    public record ErrorItem(
            int row,
            String email,
            String message
    ) {
    }
}
