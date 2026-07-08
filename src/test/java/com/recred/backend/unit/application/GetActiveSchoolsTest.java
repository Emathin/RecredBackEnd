package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetActiveSchoolsRequest;
import com.recred.backend.application.GetActiveSchoolsResponse;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private GetActiveSchools useCase;
    private Colegio firstSchool;
    private Colegio secondSchool;
    private GetActiveSchoolsResponse response;

    @BeforeEach
    void setUp() {
        useCase = new GetActiveSchools(schoolRepository);
    }

    @Test
    void givenActiveSchools_whenGettingActiveSchools_thenReturnsMappedSchools() {
        givenActiveSchools(
                school("North School"),
                school("South School"));

        whenGettingActiveSchools();

        thenSchoolsAreReturned();
        thenActiveSchoolsWereRequested();
    }

    private void givenActiveSchools(Colegio first, Colegio second) {
        firstSchool = first;
        secondSchool = second;
        when(schoolRepository.findAllActive()).thenReturn(List.of(firstSchool, secondSchool));
    }

    private void whenGettingActiveSchools() {
        response = useCase.execute(new GetActiveSchoolsRequest());
    }

    private void thenSchoolsAreReturned() {
        assertEquals(2, response.schools().size());
        assertEquals(firstSchool.getId(), response.schools().get(0).id());
        assertEquals("North School", response.schools().get(0).nombre());
        assertEquals(secondSchool.getId(), response.schools().get(1).id());
        assertEquals("South School", response.schools().get(1).nombre());
    }

    private void thenActiveSchoolsWereRequested() {
        verify(schoolRepository).findAllActive();
    }

    private Colegio school(String name) {
        Colegio school = new Colegio();
        school.setId(randomUUID());
        school.setNombre(name);
        return school;
    }
}
