package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final SpringDataUsuarioRepository springDataUsuarioRepository;

    public UsuarioRepositoryImpl(SpringDataUsuarioRepository springDataUsuarioRepository) {
        this.springDataUsuarioRepository = springDataUsuarioRepository;
    }

    @Override
    public Optional<Usuario> findById(UUID id) {
        return springDataUsuarioRepository.findById(id)
                .map(UsuarioEntity::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = toEntity(usuario);
        UsuarioEntity guardado = springDataUsuarioRepository.save(entity);
        return guardado.toDomain();
    }

    @Override
    public Optional<Usuario> findByIdCognito(String sub) {
        return springDataUsuarioRepository.findByIdCognito(sub)
                .map(UsuarioEntity::toDomain);
    }

    private UsuarioEntity toEntity(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();

        entity.setId(usuario.getId());
        entity.setIdCognito(usuario.getIdCognito());
        entity.setNombre(usuario.getNombre());
        entity.setApellido(usuario.getApellido());
        entity.setEmail(usuario.getEmail());
        entity.setTelefono(usuario.getTelefono());
        entity.setDni(usuario.getDni());
        entity.setRol(usuario.getRol());
        entity.setAutenticadoConCognito(usuario.getAutenticadoConCognito());
        entity.setActivo(usuario.getActivo());

        entity.setCreadoEn(usuario.getCreadoEn());
        entity.setActualizadoEn(usuario.getActualizadoEn());

        if (usuario.getColegio() != null && usuario.getColegio().getId() != null) {
            ColegioEntity colegioEntity = new ColegioEntity();
            colegioEntity.setId(usuario.getColegio().getId());
            entity.setColegioEntity(colegioEntity);
        }

        if (usuario.getAlumno() != null) {
            entity.setAlumnoEntity(AlumnoEntity.fromDomain(usuario.getAlumno()));
        }

        if (usuario.getTutor() != null) {
            entity.setTutorEntity(TutorEntity.fromDomain(usuario.getTutor()));
        }

        if (usuario.getKiosquero() != null) {
            entity.setKiosqueroEntity(KiosqueroEntity.fromDomain(usuario.getKiosquero()));
        }

        return entity;
    }
}
