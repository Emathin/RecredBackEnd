package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Grade;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.User;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.GradeEntity;
import com.recred.backend.infrastructure.entities.IAConsumptionProfileEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import org.springframework.stereotype.Component;

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
    public List<Student> findStudentsWithFavoriteProduct(UUID productId) {
        Map<UUID, StudentEntity> studentsById = new LinkedHashMap<>();
        springDataRepository.findByProductoEntityFavoritoId(productId)
                .stream()
                .map(IAConsumptionProfileEntity::getStudentEntity)
                .forEach(student -> studentsById.put(student.getId(), student));
        springDataRepository.findStudentsWithManualFavoriteProduct(productId)
                .forEach(student -> studentsById.put(student.getId(), student));

        return studentsById.values()
                .stream()
                .map(this::toStudentWithNotificationContext)
                .toList();
    }

    private Student toStudentWithNotificationContext(StudentEntity entity) {
        Student student = entity.toDomain();
        student.setGrade(toGrade(entity.getGradeEntity()));
        student.setTutors(entity.getTutors().stream()
                .filter(tutorEntity -> Boolean.TRUE.equals(tutorEntity.getActive()))
                .map(this::toTutor)
                .collect(java.util.stream.Collectors.toSet()));
        return student;
    }

    private Grade toGrade(GradeEntity entity) {
        if (entity == null) {
            return null;
        }
        Grade grade = new Grade();
        grade.setId(entity.getId());
        grade.setAnio(entity.getYear());
        grade.setDivision(entity.getDivision());
        grade.setActivo(entity.getActive());
        return grade;
    }

    private Tutor toTutor(TutorEntity entity) {
        Tutor tutor = new Tutor();
        tutor.setId(entity.getId());
        tutor.setActive(entity.getActive());
        tutor.setUser(toUser(entity.getUser()));
        return tutor;
    }

    private User toUser(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setName(entity.getName());
        user.setSurname(entity.getSurname());
        user.setEmail(entity.getEmail());
        user.setRole(entity.getRol());
        user.setActive(entity.getActive());
        return user;
    }
}
