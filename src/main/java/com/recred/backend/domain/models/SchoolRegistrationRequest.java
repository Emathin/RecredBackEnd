package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SchoolRegistrationRequest {

    private UUID id;
    private String schoolName;
    private String schoolEmail;
    private String schoolPhone;
    private String schoolCue;
    private Nivel schoolLevel;
    
    private String directorFirstName;
    private String directorLastName;
    private String directorEmail;
    private String directorPhone;
    private String directorDni;
    private String directorUsername;
    
    private SchoolRegistrationStatus status = SchoolRegistrationStatus.PENDING;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
