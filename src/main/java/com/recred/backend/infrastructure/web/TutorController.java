package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ActualizarFotoAlumno;
import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.GetChildrenByParent;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UserService;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final GetChildrenByParent getChildrenByParent;
    private final UserService userService;
    private final CreateStudentFromParent createStudentFromParent;
    private final ActualizarFotoAlumno actualizarFotoAlumno;
    private final CloudinaryService cloudinaryService;

    public TutorController(GetChildrenByParent getChildrenByParent, 
                           UserService userService,
                           CreateStudentFromParent createStudentFromParent,
                           ActualizarFotoAlumno actualizarFotoAlumno,
                           CloudinaryService cloudinaryService) {
        this.getChildrenByParent = getChildrenByParent;
        this.userService = userService;
        this.createStudentFromParent = createStudentFromParent;
        this.actualizarFotoAlumno = actualizarFotoAlumno;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/me/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerMisHijos(Authentication authentication) {
        List<StudentDTO> alumnos = getChildrenByParent
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
            Usuario usuario = userService.getCurrentUser(cognitoSub);
            if (usuario.getTutor() == null) {
                return ResponseEntity.ok(List.of());
            }
            id = usuario.getTutor().getId();
        } else {
            id = UUID.fromString(tutorId);
        }

        List<StudentDTO> alumnos = getChildrenByParent.execute(id)
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }

    @PostMapping(value = "/me/hijos/{alumnoId}/foto",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentDTO> subirFotoHijo(
            Authentication authentication,
            @PathVariable UUID alumnoId,
            @RequestParam("foto") MultipartFile foto) {
        String cognitoSub = cognitoSubject(authentication);
        String url = cloudinaryService.uploadImage(foto);
        com.recred.backend.domain.models.Alumno alumno =
                actualizarFotoAlumno.execute(cognitoSub, alumnoId, url);
        return ResponseEntity.ok(StudentDTO.fromDomain(alumno));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
