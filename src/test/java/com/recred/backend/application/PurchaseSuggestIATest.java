package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.SugerenciaIA;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 20, 5);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        Alumno hijo = new Alumno();
        hijo.setId(UUID.randomUUID());
        tutor.addHijo(hijo);
        padre.setTutor(tutor);

        SugerenciaIAEntity sugerenciaEntity = new SugerenciaIAEntity();
        sugerenciaEntity.setId(UUID.randomUUID());
        sugerenciaEntity.setAceptada(null);
        sugerenciaEntity.setCreadoEn(now.minusHours(1));
        sugerenciaEntity.setTitulo("Test");
        sugerenciaEntity.setMensaje("Mensaje");

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(hijo.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerenciaEntity));

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
        assertEquals(sugerenciaEntity.getId(), result.get().getId());
    }

    @Test
    void execute_Padre_HorarioDiurno_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }

    @Test
    void execute_Padre_FinDeSemana_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 21, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }

    @Test
    void execute_Alumno_EnHorarioEscolar_DeberiaRetornarSugerencia() {
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

        SugerenciaIAEntity sugerenciaEntity = new SugerenciaIAEntity();
        sugerenciaEntity.setId(UUID.randomUUID());
        sugerenciaEntity.setAceptada(null);
        sugerenciaEntity.setCreadoEn(now.minusHours(1));
        sugerenciaEntity.setTitulo("Test");
        sugerenciaEntity.setMensaje("Mensaje");

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuarioAlumno);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(alumno.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerenciaEntity));

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
        assertEquals(sugerenciaEntity.getId(), result.get().getId());
    }

    @Test
    void execute_Alumno_FueraDeHorarioEscolar_DeberiaRetornarSugerenciaDebidoAlBypass() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 15, 0);
        setupUseCase(now);

        Usuario usuarioAlumno = new Usuario();
        usuarioAlumno.setRol(RolUsuario.ALUMNO);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        usuarioAlumno.setAlumno(alumno);

        SugerenciaIAEntity sugerenciaEntity = new SugerenciaIAEntity();
        sugerenciaEntity.setId(UUID.randomUUID());
        sugerenciaEntity.setAceptada(null);
        sugerenciaEntity.setCreadoEn(now.minusHours(1));
        sugerenciaEntity.setTitulo("Test");
        sugerenciaEntity.setMensaje("Mensaje");

        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuarioAlumno);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(alumno.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerenciaEntity));

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
    }
}
