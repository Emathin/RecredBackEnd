package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataKiosqueroRepository extends
        JpaRepository<KiosqueroEntity, UUID> {

    @Query("""
            select b.id 
                from KiosqueroEntity k 
                    inner join k.buffetEntity b 
                        where k.id = CAST(:kiosqueroId AS java.util.UUID)
                        and b.vendedor = k
    """)
    UUID findBuffet(UUID kiosqueroId);

    @Query("""
            select b.id 
                from KiosqueroEntity k 
                    inner join k.buffetEntity b 
                        where k.usuario.id = CAST(:usuarioId AS java.util.UUID)
                        and b.vendedor = k
    """)
    UUID findBuffetByUsuarioId(UUID usuarioId);

    @Query("""
            select k.id 
                from KiosqueroEntity k 
                    where k.usuario.id = CAST(:usuarioId AS java.util.UUID)
    """)
    Optional<UUID> findKiosqueroIdByUsuarioId(UUID usuarioId);

    @Query("""
            select c.id
                from KiosqueroEntity k
                    inner join k.buffetEntity b
                    inner join b.colegioEntity c
                        where k.usuario.id = CAST(:usuarioId AS java.util.UUID)
                        and b.vendedor = k
    """)
    Optional<UUID> findSchoolIdByUsuarioId(UUID usuarioId);

    @Query("""
            select k.usuario.id
                from KiosqueroEntity k
                    where k.id = CAST(:kiosqueroId AS java.util.UUID)
    """)
    UUID findUsuarioId(UUID kiosqueroId);

    @Query("select k.id from KiosqueroEntity k")
    List<UUID> findAllIds();
}
