package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class UsuarioTest {

    @Test
    void asignarTutor_ShouldLinkUserAndTutor() {
        Usuario usuario = new Usuario();
        Tutor tutor = new Tutor();

        usuario.asignarTutor(tutor);

        assertSame(tutor, usuario.getTutor());
        assertSame(usuario, tutor.getUsuario());
    }

    @Test
    void asignarTutor_WhenTutorIsNull_ShouldClearTutor() {
        Usuario usuario = new Usuario();

        usuario.asignarTutor(null);

        assertNull(usuario.getTutor());
    }
}
