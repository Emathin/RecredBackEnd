package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.ObtenerHijosPorPadre;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final ObtenerHijosPorPadre obtenerHijosPorPadre;
    private final UsuarioService usuarioService;
    private final CreateStudentFromParent createStudentFromParent;

    public TutorController(ObtenerHijosPorPadre obtenerHijosPorPadre, 
                           UsuarioService usuarioService,
                           CreateStudentFromParent createStudentFromParent) {
        this.obtenerHijosPorPadre = obtenerHijosPorPadre;
        this.usuarioService = usuarioService;
        this.createStudentFromParent = createStudentFromParent;
    }

    @GetMapping("/me/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerMisHijos(Authentication authentication) {
        List<StudentDTO> alumnos = obtenerHijosPorPadre
                .executeByCognitoSubject(cognitoSubject(authentication))
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }

    @PostMapping("/me/hijos")
    public ResponseEntity<StudentDTO> crearHijo(Authentication authentication,
                                               @RequestBody CreateChildRequest request) {
        StudentDTO alumno = StudentDTO.fromDomain(createStudentFromParent.execute(
                cognitoSubject(authentication),
                request.username(),
                request.nombre(),
                request.apellido(),
                request.email(),
                request.dni(),
                request.gradoId(),
                request.urlFotoPerfil()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(alumno);
    }

    @GetMapping("/{tutorId}/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerHijos(
            @PathVariable String tutorId, 
            Authentication authentication) {
        
        UUID id;
        if ("me".equalsIgnoreCase(tutorId)) {
            String cognitoSub = authentication.getName();
            Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoSub);
            if (usuario.getTutor() == null) {
                return ResponseEntity.ok(List.of());
            }
            id = usuario.getTutor().getId();
        } else {
            id = UUID.fromString(tutorId);
        }

        List<StudentDTO> alumnos = obtenerHijosPorPadre.execute(id)
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
