package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SpringDataKiosqueroRepository extends
        JpaRepository<MerchantEntity, UUID> {

    @Query("""
            select k.buffetEntity.id 
                from MerchantEntity k 
                    where k.id = CAST(:kiosqueroId AS java.util.UUID)
    """)
    UUID findBuffet(UUID kiosqueroId);
}
