package com.recred.backend.unit.application;

import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UserService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseSuggestIATest {

    @Mock
    private UserService userService;
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
        useCase = new PurchaseSuggestIA(userService, sugerenciaIARepository, franjaHorariaRepository, clock);
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

        when(userService.getCurrentUser(cognitoId)).thenReturn(padre);
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

        when(userService.getCurrentUser(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
    }

    @Test
    void execute_Padre_FinDeSemana_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 21, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(userService.getCurrentUser(cognitoId)).thenReturn(padre);

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

        when(userService.getCurrentUser(cognitoId)).thenReturn(usuarioAlumno);
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

        when(userService.getCurrentUser(cognitoId)).thenReturn(usuarioAlumno);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(alumno.getId(), TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(sugerenciaEntity));

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
    }

    @Test
    void execute_RolNoVisible_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 21, 0);
        setupUseCase(now);

        Usuario vendedor = new Usuario();
        vendedor.setRol(RolUsuario.VENDEDOR);

        when(userService.getCurrentUser(cognitoId)).thenReturn(vendedor);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
        verifyNoInteractions(sugerenciaIARepository);
    }

    @Test
    void execute_PadreVisibleSinTutor_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 21, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);

        when(userService.getCurrentUser(cognitoId)).thenReturn(padre);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
        verifyNoInteractions(sugerenciaIARepository);
    }

    @Test
    void execute_AlumnoSinAlumnoAsociado_DeberiaRetornarVacio() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        setupUseCase(now);

        Usuario usuarioAlumno = new Usuario();
        usuarioAlumno.setRol(RolUsuario.ALUMNO);

        when(userService.getCurrentUser(cognitoId)).thenReturn(usuarioAlumno);

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertFalse(result.isPresent());
        verifyNoInteractions(sugerenciaIARepository);
    }

    @Test
    void execute_DeberiaIgnorarSugerenciasYaRespondidasYElegirLaMasReciente() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 21, 0);
        setupUseCase(now);

        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        Alumno hijo = new Alumno();
        hijo.setId(UUID.randomUUID());
        tutor.addHijo(hijo);
        padre.setTutor(tutor);

        SugerenciaIAEntity accepted = suggestion(now.minusMinutes(1));
        accepted.setAceptada(true);
        SugerenciaIAEntity olderPending = suggestion(now.minusHours(3));
        SugerenciaIAEntity latestPending = suggestion(now.minusMinutes(30));

        when(userService.getCurrentUser(cognitoId)).thenReturn(padre);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(
                hijo.getId(),
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE))
                .thenReturn(List.of(accepted, olderPending, latestPending));

        Optional<SugerenciaIA> result = useCase.execute(cognitoId);

        assertTrue(result.isPresent());
        assertEquals(latestPending.getId(), result.get().getId());
    }

    @Test
    void isVisibleParaAlumno_WhenStudentHasNoSchool_ShouldReturnFalse() {
        setupUseCase(LocalDateTime.of(2026, 6, 1, 10, 0));

        Boolean result = ReflectionTestUtils.invokeMethod(
                useCase,
                "isVisibleParaAlumno",
                new Alumno(),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );

        assertFalse(result);
    }

    @Test
    void isVisibleParaAlumno_WhenSchoolHasActiveSlotForNow_ShouldReturnTrue() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        setupUseCase(now);
        Colegio colegio = new Colegio();
        colegio.setId(UUID.randomUUID());
        Grado grado = new Grado();
        grado.setColegio(colegio);
        Alumno alumno = new Alumno();
        alumno.setGrado(grado);
        FranjaHoraria franja = new FranjaHoraria();
        franja.setActivo(true);
        franja.setHoraInicio(LocalTime.of(9, 30));
        franja.setHoraFin(LocalTime.of(10, 30));

        when(franjaHorariaRepository.findAllByColegioId(colegio.getId()))
                .thenReturn(List.of(franja));

        Boolean result = ReflectionTestUtils.invokeMethod(
                useCase,
                "isVisibleParaAlumno",
                alumno,
                now
        );

        assertTrue(result);
    }

    private SugerenciaIAEntity suggestion(LocalDateTime creadoEn) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(UUID.randomUUID());
        suggestion.setAceptada(null);
        suggestion.setCreadoEn(creadoEn);
        suggestion.setTitulo("Test");
        suggestion.setMensaje("Mensaje");
        return suggestion;
    }
}
