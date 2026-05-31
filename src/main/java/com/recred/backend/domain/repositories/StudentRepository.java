package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

    Optional<Student> findById(UUID id);

    List<Student> findAll();

    void save(Student student);

    boolean existsById(UUID alumnoId);
}
