package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Alumno;
import java.util.List;
import java.util.UUID;

public interface StudentAIProfileRepository {
    List<Alumno> findStudentsWithFavoriteProduct(UUID productId);
}
