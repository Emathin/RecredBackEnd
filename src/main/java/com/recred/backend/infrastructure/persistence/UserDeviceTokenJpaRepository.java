package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.DispositivoUsuarioEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
