package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
        List<Colegio> expectedSchools = List.of(new Colegio(), new Colegio());

        when(schoolRepository.findAllActive()).thenReturn(expectedSchools);

        List<Colegio> result = getActiveSchools.execute();

        assertEquals(expectedSchools, result);
        verify(schoolRepository).findAllActive();
    }
}
