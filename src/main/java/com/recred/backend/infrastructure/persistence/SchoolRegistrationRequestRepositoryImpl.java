package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.infrastructure.entities.SchoolRegistrationRequestEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SchoolRegistrationRequestRepositoryImpl 
        implements SchoolRegistrationRequestRepository {

    private final SpringDataSchoolRegistrationRequestRepository springDataRepository;

    @Override
    public void save(SchoolRegistrationRequest request) {
        SchoolRegistrationRequestEntity entity = 
                SchoolRegistrationRequestEntity.fromDomain(request);
        springDataRepository.save(entity);
    }

    @Override
    public Optional<SchoolRegistrationRequest> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(SchoolRegistrationRequestEntity::toDomain);
    }

    @Override
    public List<SchoolRegistrationRequest> findByStatus(SchoolRegistrationStatus status) {
        return springDataRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(SchoolRegistrationRequestEntity::toDomain)
                .toList();
    }
}
