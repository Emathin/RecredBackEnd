package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetActiveSchoolsRequest;
import com.recred.backend.application.GetActiveSchoolsResponse;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveSchoolsTest {

    @Mock
    private ColegioRepository schoolRepository;

    @InjectMocks
    private GetActiveSchools getActiveSchools;

    @Test
    void execute_ReturnsActiveSchools() {
        Colegio firstSchool = school("North School");
        Colegio secondSchool = school("South School");

        givenActiveSchools(firstSchool, secondSchool);

        GetActiveSchoolsResponse result = whenGettingActiveSchools();

        assertEquals(2, result.schools().size());
        assertEquals(firstSchool.getId(), result.schools().get(0).id());
        assertEquals("North School", result.schools().get(0).nombre());
        assertEquals(secondSchool.getId(), result.schools().get(1).id());
        assertEquals("South School", result.schools().get(1).nombre());
        verify(schoolRepository).findAllActive();
    }

    private void givenActiveSchools(Colegio... schools) {
        when(schoolRepository.findAllActive()).thenReturn(List.of(schools));
    }

    private GetActiveSchoolsResponse whenGettingActiveSchools() {
        return getActiveSchools.execute(new GetActiveSchoolsRequest());
    }

    private Colegio school(String name) {
        Colegio school = new Colegio();
        school.setId(randomUUID());
        school.setNombre(name);
        return school;
    }
}
