package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public SchoolRegistrationRequest() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getSchoolEmail() {
        return schoolEmail;
    }

    public void setSchoolEmail(String schoolEmail) {
        this.schoolEmail = schoolEmail;
    }

    public String getSchoolPhone() {
        return schoolPhone;
    }

    public void setSchoolPhone(String schoolPhone) {
        this.schoolPhone = schoolPhone;
    }

    public String getSchoolCue() {
        return schoolCue;
    }

    public void setSchoolCue(String schoolCue) {
        this.schoolCue = schoolCue;
    }

    public Nivel getSchoolLevel() {
        return schoolLevel;
    }

    public void setSchoolLevel(Nivel schoolLevel) {
        this.schoolLevel = schoolLevel;
    }

    public String getDirectorFirstName() {
        return directorFirstName;
    }

    public void setDirectorFirstName(String directorFirstName) {
        this.directorFirstName = directorFirstName;
    }

    public String getDirectorLastName() {
        return directorLastName;
    }

    public void setDirectorLastName(String directorLastName) {
        this.directorLastName = directorLastName;
    }

    public String getDirectorEmail() {
        return directorEmail;
    }

    public void setDirectorEmail(String directorEmail) {
        this.directorEmail = directorEmail;
    }

    public String getDirectorPhone() {
        return directorPhone;
    }

    public void setDirectorPhone(String directorPhone) {
        this.directorPhone = directorPhone;
    }

    public String getDirectorDni() {
        return directorDni;
    }

    public void setDirectorDni(String directorDni) {
        this.directorDni = directorDni;
    }

    public String getDirectorUsername() {
        return directorUsername;
    }

    public void setDirectorUsername(String directorUsername) {
        this.directorUsername = directorUsername;
    }

    public SchoolRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolRegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
