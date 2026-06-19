package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/alumnos")
public class AlumnoController {

    private final AlumnoRepository alumnoRepository;
    private final UserService userService;

    public AlumnoController(AlumnoRepository alumnoRepository, UserService userService) {
        this.alumnoRepository = alumnoRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AlumnoDTO>> getAllAlumnos() {
        List<AlumnoDTO> alumnos = alumnoRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/me")
    public ResponseEntity<StudentDTO> getMiPerfil(Authentication authentication) {
        String cognitoSub = authentication.getName();
        Usuario usuario = userService.getCurrentUser(cognitoSub);
        if (usuario.getAlumno() == null) {
            return ResponseEntity.notFound().build();
        }
        
        return alumnoRepository.findById(usuario.getAlumno().getId())
                .map(alumno -> ResponseEntity.ok(StudentDTO.fromDomain(alumno)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AlumnoDTO toDto(Alumno alumno) {
        return new AlumnoDTO(
                alumno.getId(),
                alumno.getNombre(),
                alumno.getApellido(),
                alumno.getUsuario() != null ? alumno.getUsuario().getDni() : null
        );
    }

    public record AlumnoDTO(
            java.util.UUID id,
            String nombre,
            String apellido,
            String dni
    ) {}
}
