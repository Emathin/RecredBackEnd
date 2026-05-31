package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Student;

import java.util.List;
import java.util.UUID;

public interface StudentAIProfileRepository {
    List<Student> findStudentsWithFavoriteProduct(UUID productId);
}
