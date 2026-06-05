package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseSuggestIATest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SugerenciaIARepository sugerenciaIARepository;
    @Mock
    private FranjaHorariaRepository franjaHorariaRepository;

    private PurchaseSuggestIA useCase;
    private String cognitoId = "user-123";

    @BeforeEach
    void setUp() {
    }

    private void setupUseCase(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        useCase = new PurchaseSuggestIA(usuarioService, sugerenciaIARepository, franjaHorariaRepository, clock);
    }

    @Test
    void execute_Padre_HorarioNocturnoDiaHabil_DeberiaRetornarSugerencia() {
        // Lunes 20:05
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 20, 5);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        Alumno hijo = new Alumno();
        hijo.setId(UUID.randomUUID());
        tutor.addHijo(hijo);
        padre.setTutor(tutor);

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setAceptada(null);
        sugerencia.setCreadoEn(now.minusHours(1));

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(hijo.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerencia));

        Optional<SugerenciaIAEntity> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
        assertEquals(sugerencia, result.get());
    }

    @Test
    void execute_Padre_HorarioDiurno_DeberiaRetornarVacio() {
        // Lunes 10:00
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIAEntity> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }

    @Test
    void execute_Padre_FinDeSemana_DeberiaRetornarVacio() {
        // Sabado 21:00
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 21, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIAEntity> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }

    @Test
    void execute_Alumno_EnHorarioEscolar_DeberiaRetornarSugerencia() {
        // Lunes 10:00
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        setupUseCase(now);

        Colegio colegio = new Colegio();
        colegio.setId(UUID.randomUUID());
        Grado grado = new Grado();
        grado.setColegio(colegio);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setGrado(grado);
        Usuario usuarioAlumno = new Usuario();
        usuarioAlumno.setRol(RolUsuario.ALUMNO);
        usuarioAlumno.setAlumno(alumno);

        FranjaHoraria franja = new FranjaHoraria();
        franja.setActivo(true);
        franja.setHoraInicio(LocalTime.of(8, 0));
        franja.setHoraFin(LocalTime.of(12, 0));

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setAceptada(null);
        sugerencia.setCreadoEn(now.minusHours(1));

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuarioAlumno);
        when(franjaHorariaRepository.findAllByColegioId(colegio.getId())).thenReturn(List.of(franja));
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(alumno.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerencia));

        Optional<SugerenciaIAEntity> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
        assertEquals(sugerencia, result.get());
    }

    @Test
    void execute_Alumno_FueraDeHorarioEscolar_DeberiaRetornarVacio() {
        // Lunes 15:00
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 15, 0);
        setupUseCase(now);

        Colegio colegio = new Colegio();
        colegio.setId(UUID.randomUUID());
        Grado grado = new Grado();
        grado.setColegio(colegio);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setGrado(grado);
        Usuario usuarioAlumno = new Usuario();
        usuarioAlumno.setRol(RolUsuario.ALUMNO);
        usuarioAlumno.setAlumno(alumno);

        FranjaHoraria franja = new FranjaHoraria();
        franja.setActivo(true);
        franja.setHoraInicio(LocalTime.of(8, 0));
        franja.setHoraFin(LocalTime.of(12, 0));

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuarioAlumno);
        when(franjaHorariaRepository.findAllByColegioId(colegio.getId())).thenReturn(List.of(franja));

        Optional<SugerenciaIAEntity> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }
}
