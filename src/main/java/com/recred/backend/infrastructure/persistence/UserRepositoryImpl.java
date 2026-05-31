package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.User;
import com.recred.backend.domain.repositories.UserRepository;
import com.recred.backend.infrastructure.entities.SchoolEntity;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import com.recred.backend.infrastructure.entities.MerchantEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUsuarioRepository springDataUsuarioRepository;

    public UserRepositoryImpl(SpringDataUsuarioRepository springDataUsuarioRepository) {
        this.springDataUsuarioRepository = springDataUsuarioRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUsuarioRepository.findById(id)
                .map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity guardado = springDataUsuarioRepository.save(entity);
        return guardado.toDomain();
    }

    @Override
    public Optional<User> findByIdCognito(String sub) {
        return springDataUsuarioRepository.findByIdCognito(sub)
                .map(UserEntity::toDomain);
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();

        entity.setId(user.getId());
        entity.setIdCognito(user.getIdCognito());
        entity.setName(user.getName());
        entity.setSurname(user.getSurname());
        entity.setEmail(user.getEmail());
        entity.setTelephone(user.getTelephone());
        entity.setDni(user.getDni());
        entity.setRol(user.getRole());
        entity.setCheckedByCognito(user.getCheckedByCognito());
        entity.setActive(user.getActive());

        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());

        if (user.getSchool() != null && user.getSchool().getId() != null) {
            SchoolEntity schoolEntity = new SchoolEntity();
            schoolEntity.setId(user.getSchool().getId());
            entity.setSchoolEntity(schoolEntity);
        }

        if (user.getStudent() != null) {
            entity.setStudentEntity(StudentEntity.fromDomain(user.getStudent()));
        }

        if (user.getTutor() != null) {
            entity.setTutorEntity(TutorEntity.fromDomain(user.getTutor()));
        }

        if (user.getMerchant() != null) {
            entity.setKiosqueroEntity(MerchantEntity.fromDomain(user.getMerchant()));
        }

        return entity;
    }
}
