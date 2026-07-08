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

    public TutorInvitationImportResult execute(String directorCognitoSubject, String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new IllegalArgumentException("El archivo CSV esta vacio");
        }

        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV esta vacio");
        }

        Header header = parseHeader(lines.get(0));
        Set<String> seenEmails = new HashSet<>();
        int created = 0;
        int resent = 0;
        int alreadyAssociated = 0;
        List<TutorInvitationImportError> errors = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            int row = i + 1;
            List<String> values = parseCsvLine(line);
            String rawEmail = value(values, header.emailIndex());
            String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) {
                errors.add(new TutorInvitationImportError(row, email, "El email es obligatorio"));
                continue;
            }
            if (!seenEmails.add(email)) {
                errors.add(new TutorInvitationImportError(row, email,
                        "El email esta duplicado en el archivo"));
                continue;
            }
            try {
                InviteTutorToSchool.TutorInvitationResult result = inviteTutorToSchool.execute(
                        directorCognitoSubject,
                        new InviteTutorToSchool.TutorInvitationCommand(
                                email,
                                value(values, header.firstNameIndex()),
                                value(values, header.lastNameIndex()),
                                value(values, header.phoneIndex())
                        ));
                if ("CREATED".equals(result.status())
                        || "CREATED_EMAIL_ERROR".equals(result.status())) {
                    created++;
                } else if ("RESENT".equals(result.status())
                        || "RESENT_EMAIL_ERROR".equals(result.status())) {
                    resent++;
                } else if ("ALREADY_ASSOCIATED".equals(result.status())) {
                    alreadyAssociated++;
                }
            } catch (RuntimeException exception) {
                errors.add(new TutorInvitationImportError(row, email, exception.getMessage()));
            }
        }

        return new TutorInvitationImportResult(
                Math.max(lines.size() - 1, 0),
                created,
                resent,
                alreadyAssociated,
                errors);
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
}
