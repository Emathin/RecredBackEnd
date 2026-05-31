package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    private UUID id;
    private User user;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<Student> children = new HashSet<>();

    public void addChild(Student student) {
        if (student != null) {
            this.children.add(student);
            student.getTutors().add(this);
        }
    }

    public void removeChild(Student student) {
        if (student != null) {
            this.children.remove(student);
            student.getTutors().remove(this);
        }
    }
}