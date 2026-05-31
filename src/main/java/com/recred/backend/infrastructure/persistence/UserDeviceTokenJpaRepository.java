package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.UserDeviceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceTokenJpaRepository
        extends JpaRepository<UserDeviceEntity, UUID> {

    @Query("""
            select d.fcmToken
            from UserDeviceEntity d
            where d.userEntity.id = :userId
            and d.active = true
            """)
    List<String> findActiveTokensByUserId(@Param("userId") UUID userId);
}
