package com.recred.backend.unit.application;

import com.recred.backend.application.GetSchoolOverview;
import com.recred.backend.application.GetSchoolOverviewRequest;
import com.recred.backend.application.GetSchoolOverviewResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSchoolOverviewTest {

    private static final String COGNITO_SUBJECT = "test-subject";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private BuffetRepository buffetRepository;

    private GetSchoolOverview useCase;

    private UUID schoolId;
    private UUID buffetId;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        useCase = new GetSchoolOverview(
                usuarioRepository,
                colegioRepository,
                buffetRepository,
                CLOCK);
        schoolId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        vendedorId = UUID.randomUUID();
    }

    @Test
    void givenValidUserAndSchool_whenExecute_thenReturnsOverview() {
        givenValidUserAndSchool();

        GetSchoolOverviewResponse response = whenGetSchoolOverview();

        thenReturnsOverviewWithBuffetData(response);
    }

    @Test
    void givenUserNotFound_whenExecute_thenThrowsException() {
        givenUserNotFound();

        Executable execution = this::whenGetSchoolOverview;

        thenThrowsUserNotFoundException(execution);
    }

    @Test
    void givenUserWithoutSchool_whenExecute_thenThrowsException() {
        givenUserWithoutSchool();

        Executable execution = this::whenGetSchoolOverview;

        thenThrowsExceptionForUserWithoutSchool(execution);
    }

    @Test
    void givenSchoolNotFound_whenExecute_thenThrowsException() {
        givenSchoolNotFound();

        Executable execution = this::whenGetSchoolOverview;

        thenThrowsSchoolNotFoundException(execution);
    }

    @Test
    void givenBuffetWithoutVendedor_whenExecute_thenMapsVendedorToNull() {
        givenBuffetWithoutVendedor();

        GetSchoolOverviewResponse response = whenGetSchoolOverview();

        thenReturnsOverviewWithNullVendedor(response);
    }

    private void givenValidUserAndSchool() {
        Usuario usuario = buildUserWithSchool();
        Colegio colegio = buildSchool();
        Buffet buffet = buildBuffetWithVendedor();

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));
        when(buffetRepository.findByColegioId(schoolId)).thenReturn(List.of(buffet));
    }

    private void givenUserNotFound() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenUserWithoutSchool() {
        Usuario usuario = new Usuario();
        usuario.setColegio(null);
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
    }

    private void givenSchoolNotFound() {
        Usuario usuario = buildUserWithSchool();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.empty());
    }

    private void givenBuffetWithoutVendedor() {
        Usuario usuario = buildUserWithSchool();
        Colegio colegio = buildSchool();
        Buffet buffet = buildBuffetWithoutVendedor();

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));
        when(buffetRepository.findByColegioId(schoolId)).thenReturn(List.of(buffet));
    }

    private GetSchoolOverviewResponse whenGetSchoolOverview() {
        return useCase.execute(new GetSchoolOverviewRequest(COGNITO_SUBJECT));
    }

    private void thenReturnsOverviewWithBuffetData(GetSchoolOverviewResponse response) {
        assertEquals(schoolId, response.id());
        assertEquals("Colegio Test", response.nombre());
        assertEquals("12345", response.cue());
        assertEquals(1, response.buffets().size());

        GetSchoolOverviewResponse.BuffetDto buffetDto = response.buffets().get(0);
        assertEquals(buffetId, buffetDto.id());
        assertEquals("Buffet Test", buffetDto.nombre());
        assertEquals(true, buffetDto.activo());

        GetSchoolOverviewResponse.VendedorDto vendedorDto = buffetDto.vendedor();
        assertEquals(vendedorId, vendedorDto.id());
        assertEquals("Juan", vendedorDto.nombre());
        assertEquals("Perez", vendedorDto.apellido());
        assertEquals("juan@test.com", vendedorDto.email());
        assertEquals("20-12345678-9", vendedorDto.cuit());
    }

    private void thenThrowsUserNotFoundException(Executable execution) {
        assertThrows(UsuarioAutenticadoNoEncontradoException.class, execution);
        verifyNoInteractions(colegioRepository, buffetRepository);
    }

    private void thenThrowsExceptionForUserWithoutSchool(Executable execution) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("El usuario no tiene un colegio asociado.", exception.getMessage());
        verifyNoInteractions(colegioRepository, buffetRepository);
    }

    private void thenThrowsSchoolNotFoundException(Executable execution) {
        assertThrows(SchoolNotFoundException.class, execution);
        verifyNoInteractions(buffetRepository);
    }

    private void thenReturnsOverviewWithNullVendedor(GetSchoolOverviewResponse response) {
        assertEquals(1, response.buffets().size());
        assertNull(response.buffets().get(0).vendedor());
    }

    private Usuario buildUserWithSchool() {
        Usuario usuario = new Usuario();
        Colegio colegio = buildSchool();
        usuario.setColegio(colegio);
        return usuario;
    }

    private Colegio buildSchool() {
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        colegio.setNombre("Colegio Test");
        colegio.setCue("12345");
        return colegio;
    }

    private Buffet buildBuffetWithVendedor() {
        Buffet buffet = buildBuffetWithoutVendedor();
        Kiosquero vendedor = new Kiosquero();
        vendedor.setId(vendedorId);
        vendedor.setCuit("20-12345678-9");
        Usuario vendedorUsuario = new Usuario();
        vendedorUsuario.setNombre("Juan");
        vendedorUsuario.setApellido("Perez");
        vendedorUsuario.setEmail("juan@test.com");
        vendedor.setUsuario(vendedorUsuario);
        buffet.setVendedor(vendedor);
        return buffet;
    }

    private Buffet buildBuffetWithoutVendedor() {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setNombre("Buffet Test");
        buffet.setActivo(true);
        return buffet;
    }
}
