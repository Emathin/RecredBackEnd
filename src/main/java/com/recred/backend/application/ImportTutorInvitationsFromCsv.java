package com.recred.backend.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ImportTutorInvitationsFromCsv {

    private final InviteTutorToSchool inviteTutorToSchool;

    public ImportTutorInvitationsFromCsv(InviteTutorToSchool inviteTutorToSchool) {
        this.inviteTutorToSchool = inviteTutorToSchool;
    }

    public TutorInvitationImportResult execute(ImportTutorInvitationsFromCsvRequest request) {
        List<String> lines = splitIntoLines(request.csvContent());
        Header header = parseHeader(lines.get(0));
        
        ImportContext context = processAllRows(request.directorCognitoSubject(), lines, header);

        return buildResult(lines, context);
    }

    private List<String> splitIntoLines(String csvContent) {
        return csvContent.lines().toList();
    }

    private ImportContext processAllRows(
            String directorCognitoSubject, List<String> lines, Header header) {
        ImportContext context = new ImportContext();
        for (int i = 1; i < lines.size(); i++) {
            processRow(directorCognitoSubject, lines.get(i), i + 1, header, context);
        }
        return context;
    }

    private TutorInvitationImportResult buildResult(List<String> lines, ImportContext context) {
        return new TutorInvitationImportResult(
                Math.max(lines.size() - 1, 0),
                context.created,
                context.resent,
                context.alreadyAssociated,
                context.errors);
    }

    private void processRow(
            String directorCognitoSubject, String line, int row, 
            Header header, ImportContext context) {
        if (line == null || line.isBlank()) {
            return;
        }

        List<String> values = parseCsvLine(line);
        String email = extractEmail(values, header);

        if (email.isBlank()) {
            context.errors.add(
                    new TutorInvitationImportError(row, email, "El email es obligatorio"));
            return;
        }

        if (!context.seenEmails.add(email)) {
            context.errors.add(
                    new TutorInvitationImportError(
                            row, email, "El email esta duplicado en el archivo"));
            return;
        }

        inviteAndAccumulateResult(directorCognitoSubject, values, email, row, header, context);
    }

    private String extractEmail(List<String> values, Header header) {
        String rawEmail = value(values, header.emailIndex());
        return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void inviteAndAccumulateResult(
            String directorCognitoSubject, List<String> values, String email, 
            int row, Header header, ImportContext context) {
        try {
            InviteTutorToSchool.TutorInvitationResult result = inviteTutorToSchool.execute(
                    directorCognitoSubject,
                    new InviteTutorToSchool.TutorInvitationCommand(
                            email,
                            value(values, header.firstNameIndex()),
                            value(values, header.lastNameIndex()),
                            value(values, header.phoneIndex())
                    ));
            
            accumulateResult(result.status(), context);
        } catch (RuntimeException exception) {
            context.errors.add(new TutorInvitationImportError(row, email, exception.getMessage()));
        }
    }

    private void accumulateResult(String status, ImportContext context) {
        if ("CREATED".equals(status) || "CREATED_EMAIL_ERROR".equals(status)) {
            context.created++;
        } else if ("RESENT".equals(status) || "RESENT_EMAIL_ERROR".equals(status)) {
            context.resent++;
        } else if ("ALREADY_ASSOCIATED".equals(status)) {
            context.alreadyAssociated++;
        }
    }

    private Header parseHeader(String headerLine) {
        List<String> columns = parseCsvLine(headerLine).stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
        int email = columns.indexOf("email");
        if (email < 0) {
            throw new IllegalArgumentException(
                    "El archivo CSV debe incluir una columna email");
        }
        return new Header(
                email,
                columns.indexOf("nombre") >= 0 ? columns.indexOf("nombre")
                        : columns.indexOf("firstName".toLowerCase(Locale.ROOT)),
                columns.indexOf("apellido") >= 0 ? columns.indexOf("apellido")
                        : columns.indexOf("lastName".toLowerCase(Locale.ROOT)),
                columns.indexOf("telefono") >= 0 ? columns.indexOf("telefono")
                        : columns.indexOf("phone"));
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String value(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Header(int emailIndex, int firstNameIndex, int lastNameIndex, int phoneIndex) {
    }

    public record TutorInvitationImportResult(
            int totalRows,
            int createdInvitations,
            int resentInvitations,
            int alreadyAssociated,
            List<TutorInvitationImportError> errors
    ) {
    }

    public record TutorInvitationImportError(
            int row,
            String email,
            String message
    ) {
    }

    private static class ImportContext {
        Set<String> seenEmails = new HashSet<>();
        int created = 0;
        int resent = 0;
        int alreadyAssociated = 0;
        List<TutorInvitationImportError> errors = new ArrayList<>();
    }
}
