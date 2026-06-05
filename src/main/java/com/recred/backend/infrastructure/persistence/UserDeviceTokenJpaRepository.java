package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.DispositivoUsuarioEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceTokenJpaRepository
        extends JpaRepository<DispositivoUsuarioEntity, UUID> {

    @Query("""
            select d.fcmToken
            from DispositivoUsuarioEntity d
            where d.usuarioEntity.id = :userId
            and d.activo = true
            """)
    List<String> findActiveTokensByUserId(@Param("userId") UUID userId);

    Optional<DispositivoUsuarioEntity> findByFcmToken(String fcmToken);

    @Modifying
    @Query("UPDATE DispositivoUsuarioEntity d SET d.activo = false WHERE d.fcmToken IN :tokens")
    void deactivateTokens(@Param("tokens") List<String> tokens);
}
