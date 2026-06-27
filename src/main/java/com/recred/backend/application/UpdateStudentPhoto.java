package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdateStudentPhoto {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;

    public UpdateStudentPhoto(UsuarioRepository usuarioRepository,
                              TutorRepository tutorRepository,
                              AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public UpdateStudentPhotoResponse execute(UpdateStudentPhotoRequest request) {
        Usuario parent = this.findUsuarioByCognito(request.parentCognitoSubject());

        this.analizarRolUsuario(parent);

        Tutor tutor = this.findTutorByUsuarioId(parent.getId());
        Alumno alumno = this.findAlumnoById(request.alumnoId());

        boolean esHijo = this.isTutorOfStudent(tutor, alumno);
        this.analizarExcepcion(esHijo);

        String idCognitoAlumno = alumno.getUsuario().getIdCognito();
        usuarioRepository.updateFotoPerfil(idCognitoAlumno, request.urlFotoPerfil());

        alumno.getUsuario().setUrlFotoPerfil(request.urlFotoPerfil());

        return toResponse(alumno);
    }

    private void analizarRolUsuario(Usuario parent){
        if (parent.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario autenticado no tiene rol PADRE");
        }

    }

    private void analizarExcepcion(boolean esHijo){
        if (!esHijo) {
            throw new AccessDeniedException(
                    "El alumno no pertenece al tutor autenticado");
        }
    }

    private Usuario findUsuarioByCognito(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado"));
    }

    private Tutor findTutorByUsuarioId(UUID usuarioId) {
        return tutorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));
    }

    private Alumno findAlumnoById(UUID id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new AlumnoNoEncontradoException(id));
    }

    private boolean isTutorOfStudent(Tutor tutor, Alumno alumno) {
        return tutor.getHijos().stream()
                .anyMatch(hijo -> hijo.getId().equals(alumno.getId()));
    }

    private UpdateStudentPhotoResponse toResponse(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ?
                alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : "Sin apellido";
        String urlFotoPerfil = alumno.getUsuario() != null ?
                alumno.getUsuario().getUrlFotoPerfil() : null;

        String grado = null;
        java.util.UUID colegioId = null;
        if (alumno.getGrado() != null) {
            grado = alumno.getGrado().getAnio() + " " + alumno.getGrado().getDivision();
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null ?
                alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;

        return new UpdateStudentPhotoResponse(
                alumno.getId(),
                nombre,
                apellido,
                alumno.getActivo(),
                grado,
                colegioId,
                saldo,
                urlFotoPerfil
        );
    }
}
