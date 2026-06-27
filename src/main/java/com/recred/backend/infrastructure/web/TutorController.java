package com.recred.backend.infrastructure.web;

import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.application.UpdateStudentPhotoRequest;
import com.recred.backend.application.UpdateStudentPhotoResponse;
import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.GetChildrenByTutor;
import com.recred.backend.application.GetChildrenByTutorRequest;
import com.recred.backend.application.GetChildrenByTutorResponse;
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

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;


@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final GetChildrenByTutor getChildrenByTutor;
    private final UsuarioService usuarioService;
    private final CreateStudentFromParent createStudentFromParent;
    private final UpdateStudentPhoto updateStudentPhoto;
    private final CloudinaryService cloudinaryService;

    public TutorController(GetChildrenByTutor getChildrenByTutor,
                           UsuarioService usuarioService,
                           CreateStudentFromParent createStudentFromParent,
                           UpdateStudentPhoto updateStudentPhoto,
                           CloudinaryService cloudinaryService) {
        this.getChildrenByTutor = getChildrenByTutor;
        this.usuarioService = usuarioService;
        this.createStudentFromParent = createStudentFromParent;
        this.updateStudentPhoto = updateStudentPhoto;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/me/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerMisHijos(Authentication authentication) {
        var request = GetChildrenByTutorRequest.byCognitoSubject(cognitoSubject(authentication));
        List<StudentDTO> alumnos = getChildrenByTutor
                .execute(request)
                .stream()
                .map(this::toStudentDTO)
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

        var request = GetChildrenByTutorRequest.byTutorId(id);
        List<StudentDTO> alumnos = getChildrenByTutor.execute(request)
                .stream()
                .map(this::toStudentDTO)
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
        UpdateStudentPhotoRequest request =
                new UpdateStudentPhotoRequest(cognitoSub, alumnoId, url);
        UpdateStudentPhotoResponse response = updateStudentPhoto.execute(request);
        return ResponseEntity.ok(toStudentDTO(response));
    }

    private StudentDTO toStudentDTO(UpdateStudentPhotoResponse response) {
        return new StudentDTO(
                response.id(),
                response.nombre(),
                response.apellido(),
                response.activo(),
                response.grado(),
                response.colegioId(),
                response.saldo(),
                response.urlFotoPerfil()
        );
    }

    private StudentDTO toStudentDTO(GetChildrenByTutorResponse response) {
        return new StudentDTO(
                response.id(),
                response.nombre(),
                response.apellido(),
                response.activo(),
                response.grado(),
                response.colegioId(),
                response.saldo(),
                response.urlFotoPerfil()
        );
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
