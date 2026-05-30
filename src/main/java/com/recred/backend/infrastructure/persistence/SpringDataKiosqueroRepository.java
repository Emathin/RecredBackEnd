package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SpringDataKiosqueroRepository extends
        JpaRepository<KiosqueroEntity, UUID> {

    @Query("""
            select k.buffetEntity.id 
                from KiosqueroEntity k 
                    where k.id = CAST(:kiosqueroId AS java.util.UUID)
    """)
    UUID findBuffet(UUID kiosqueroId);
}
