package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Student {

    private UUID id;
    private User user;
    private Boolean active = true;
    private Grade grade;
    private Set<Tutor> tutors = new HashSet<>();
    private Wallet wallet;
    private Set<HealthClassification> healthRestrictions = new HashSet<>();
    private Set<Product> favouriteProducts = new HashSet<>();
    private Set<TimeRestriction> timeRestrictions = new HashSet<>();

    public Student(UUID id, User user, Boolean active, Grade grade,
                   Set<HealthClassification> healthRestrictions) {
        this.id = id;
        this.user = user;
        this.active = active;
        this.grade = grade;
        this.healthRestrictions = healthRestrictions != null ? healthRestrictions : new HashSet<>();
    }

    public void addHealthRestriction(HealthClassification restriction) {
        if (restriction != null) {
            boolean yaExiste = this.healthRestrictions.stream()
                    .anyMatch(r -> r.getId().equals(restriction.getId()));

            if (!yaExiste) {
                this.healthRestrictions.add(restriction);
            }
        }
    }

    public void removeHealthRestriction(UUID restrictionId) {
        this.healthRestrictions.removeIf(r -> r.getId().equals(restrictionId));
    }

    public void addTimeRestriction(TimeRestriction restriction) {
        this.timeRestrictions.add(restriction);
    }

    public void removeTimeRestriction(TimeRestriction restriction) {
        this.timeRestrictions.remove(restriction);
    }

    public void unsubscribe() {
        this.active = false;
    }
}