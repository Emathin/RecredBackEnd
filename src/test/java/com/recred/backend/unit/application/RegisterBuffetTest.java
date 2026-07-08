package com.recred.backend.unit.application;

import com.recred.backend.application.RegisterBuffet;
import com.recred.backend.application.RegisterBuffetRequest;
import com.recred.backend.application.RegisterBuffetResponse;
import com.recred.backend.domain.exceptions.BuffetAlreadyExistsForSchoolException;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.ColegioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterBuffetTest {

    private ColegioRepository colegioRepository;
    private BuffetRepository buffetRepository;
    private RegisterBuffet registerBuffet;

    @BeforeEach
    void setUp() {
        colegioRepository = mock(ColegioRepository.class);
        buffetRepository = mock(BuffetRepository.class);
        registerBuffet = new RegisterBuffet(colegioRepository, buffetRepository);
    }

    @Test
    void execute_shouldRegisterBuffet_whenSchoolExists() {
        UUID schoolId = UUID.randomUUID();
        RegisterBuffetRequest req = new RegisterBuffetRequest(
                schoolId, "Buffet Test", LocalDate.now().plusYears(1));

        Colegio colegio = new Colegio();
        colegio.setId(schoolId);

        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));

        Buffet savedBuffet = new Buffet();
        savedBuffet.setId(UUID.randomUUID());
        when(buffetRepository.save(any(Buffet.class))).thenAnswer(invocation -> {
            Buffet b = invocation.getArgument(0);
            b.setId(savedBuffet.getId());
            return b;
        });

        RegisterBuffetResponse response = registerBuffet.execute(req);

        assertNotNull(response.buffetId());
        verify(buffetRepository).save(any(Buffet.class));
    }

    @Test
    void execute_shouldThrowSchoolNotFoundException_whenSchoolDoesNotExist() {
        UUID schoolId = UUID.randomUUID();
        RegisterBuffetRequest req = new RegisterBuffetRequest(
                schoolId, "Buffet Test", LocalDate.now().plusYears(1));

        when(colegioRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(SchoolNotFoundException.class, () -> registerBuffet.execute(req));
    }

    @Test
    void execute_shouldThrowException_whenSchoolAlreadyHasBuffet() {
        UUID schoolId = UUID.randomUUID();
        RegisterBuffetRequest req = new RegisterBuffetRequest(
                schoolId, "Buffet Test", LocalDate.now().plusYears(1));

        Colegio colegio = new Colegio();
        colegio.setId(schoolId);

        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));
        when(buffetRepository.existsByColegioId(schoolId)).thenReturn(true);

        assertThrows(BuffetAlreadyExistsForSchoolException.class,
                () -> registerBuffet.execute(req));
        verify(buffetRepository, never()).save(any(Buffet.class));
    }
}
