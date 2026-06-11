package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUsuarioRepository extends
        JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByIdCognito(String idCognito);

    Optional<UsuarioEntity> findByEmail(String email);

}
