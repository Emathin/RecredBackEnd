package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.PerfilConsumoIAEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StudentAIProfileRepositoryImpl implements StudentAIProfileRepository {

    private final SpringDataPerfilConsumoIARepository springDataRepository;

    public StudentAIProfileRepositoryImpl(
            SpringDataPerfilConsumoIARepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alumno> findStudentsWithFavoriteProduct(UUID productId) {
        Map<UUID, AlumnoEntity> studentsById = new LinkedHashMap<>();
        springDataRepository.findByProductoEntityFavoritoId(productId)
                .stream()
                .map(PerfilConsumoIAEntity::getAlumnoEntity)
                .forEach(student -> studentsById.put(student.getId(), student));
        springDataRepository.findStudentsWithManualFavoriteProduct(productId)
                .forEach(student -> studentsById.put(student.getId(), student));

        return studentsById.values()
                .stream()
                .map(this::toStudentWithNotificationContext)
                .toList();
    }

    private Alumno toStudentWithNotificationContext(AlumnoEntity entity) {
        Alumno alumno = entity.toDomain();
        alumno.setGrado(toGrade(entity.getGradoEntity()));
        alumno.setTutores(entity.getTutores().stream()
                .filter(tutorEntity -> Boolean.TRUE.equals(tutorEntity.getActiva()))
                .map(this::toTutor)
                .collect(java.util.stream.Collectors.toSet()));
        return alumno;
    }

    private Grado toGrade(GradoEntity entity) {
        if (entity == null) {
            return null;
        }
        Grado grado = new Grado();
        grado.setId(entity.getId());
        grado.setAnio(entity.getAnio());
        grado.setDivision(entity.getDivision());
        grado.setActivo(entity.getActivo());
        return grado;
    }

    private Tutor toTutor(TutorEntity entity) {
        Tutor tutor = new Tutor();
        tutor.setId(entity.getId());
        tutor.setActiva(entity.getActiva());
        tutor.setUsuario(toUser(entity.getUsuario()));
        return tutor;
    }

    private Usuario toUser(UsuarioEntity entity) {
        Usuario usuario = new Usuario();
        usuario.setId(entity.getId());
        usuario.setNombre(entity.getNombre());
        usuario.setApellido(entity.getApellido());
        usuario.setEmail(entity.getEmail());
        usuario.setRol(entity.getRol());
        usuario.setActivo(entity.getActivo());
        usuario.setPlan(entity.getPlan());
        return usuario;
    }
}
