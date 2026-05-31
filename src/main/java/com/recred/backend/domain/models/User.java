package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class User {

    private UUID id;
    private School school;
    private String idCognito;
    private String name;
    private String surname;
    private String email;
    private String telephone;
    private String dni;
    private UserRole role;
    private Boolean checkedByCognito = false;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Student student;
    private Tutor tutor;
    private Merchant merchant;

    public void asignarTutor(Tutor tutor) {
        this.tutor = tutor;
        if (tutor != null) {
            tutor.setUser(this);
        }
    }
}