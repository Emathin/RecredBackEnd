package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    private UUID id;
    private Colegio colegio;
    private String idCognito;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String dni;
    private RolUsuario rol;
    private Boolean autenticadoConCognito = false;
    private Boolean activo = true;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private String urlFotoPerfil;
    private TipoPlan plan = TipoPlan.GRATUITO;
    private Alumno alumno;
    private Tutor tutor;
    private Kiosquero kiosquero;

    public TipoPlan getPlan() {
        return plan != null ? plan : TipoPlan.GRATUITO;
    }

    public void asignarTutor(Tutor tutor) {
        this.tutor = tutor;
        if (tutor != null) {
            tutor.setUsuario(this);
        }
    }
}