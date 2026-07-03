package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TutorSchool {

    private UUID id;
    private UUID tutorId;
    private Colegio school;
    private TutorSchoolStatus status = TutorSchoolStatus.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
