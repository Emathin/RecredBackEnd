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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private BuffetRepository buffetRepository;

    private GetSchoolOverview useCase;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        useCase = new GetSchoolOverview(usuarioRepository, colegioRepository, buffetRepository);
        schoolId = UUID.randomUUID();
    }

    @Test
    void givenValidUserAndSchool_whenExecute_thenReturnsOverview() {
        Usuario usuario = new Usuario();
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        colegio.setNombre("Colegio Test");
        colegio.setCue("12345");
        usuario.setColegio(colegio);

        Buffet buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre("Buffet Test");
        buffet.setActivo(true);

        Kiosquero vendedor = new Kiosquero();
        vendedor.setId(UUID.randomUUID());
        vendedor.setCuit("20-12345678-9");
        
        Usuario vendedorUsuario = new Usuario();
        vendedorUsuario.setNombre("Juan");
        vendedorUsuario.setApellido("Perez");
        vendedorUsuario.setEmail("juan@test.com");
        vendedor.setUsuario(vendedorUsuario);
        buffet.setVendedor(vendedor);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));
        when(buffetRepository.findByColegioId(schoolId)).thenReturn(List.of(buffet));

        GetSchoolOverviewRequest request = new GetSchoolOverviewRequest(COGNITO_SUBJECT);

        GetSchoolOverviewResponse response = useCase.execute(request);

        assertEquals(schoolId, response.id());
        assertEquals("Colegio Test", response.nombre());
        assertEquals("12345", response.cue());
        assertEquals(1, response.buffets().size());
        
        GetSchoolOverviewResponse.BuffetDto buffetDto = response.buffets().get(0);
        assertEquals(buffet.getId(), buffetDto.id());
        assertEquals("Buffet Test", buffetDto.nombre());
        assertEquals(true, buffetDto.activo());
        
        GetSchoolOverviewResponse.VendedorDto vendedorDto = buffetDto.vendedor();
        assertEquals(vendedor.getId(), vendedorDto.id());
        assertEquals("Juan", vendedorDto.nombre());
        assertEquals("Perez", vendedorDto.apellido());
        assertEquals("juan@test.com", vendedorDto.email());
        assertEquals("20-12345678-9", vendedorDto.cuit());
    }

    @Test
    void givenUserNotFound_whenExecute_thenThrowsException() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        GetSchoolOverviewRequest request = new GetSchoolOverviewRequest(COGNITO_SUBJECT);

        assertThrows(UsuarioAutenticadoNoEncontradoException.class, 
                () -> useCase.execute(request));
        
        verifyNoInteractions(colegioRepository, buffetRepository);
    }

    @Test
    void givenUserWithoutSchool_whenExecute_thenThrowsException() {
        Usuario usuario = new Usuario();
        usuario.setColegio(null);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));

        GetSchoolOverviewRequest request = new GetSchoolOverviewRequest(COGNITO_SUBJECT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> useCase.execute(request));
        
        assertEquals("El usuario no tiene un colegio asociado.", exception.getMessage());
        verifyNoInteractions(colegioRepository, buffetRepository);
    }

    @Test
    void givenSchoolNotFound_whenExecute_thenThrowsException() {
        Usuario usuario = new Usuario();
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        usuario.setColegio(colegio);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.empty());

        GetSchoolOverviewRequest request = new GetSchoolOverviewRequest(COGNITO_SUBJECT);

        assertThrows(SchoolNotFoundException.class, () -> useCase.execute(request));
        
        verifyNoInteractions(buffetRepository);
    }

    @Test
    void givenBuffetWithoutVendedor_whenExecute_thenMapsVendedorToNull() {
        Usuario usuario = new Usuario();
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        usuario.setColegio(colegio);

        Buffet buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        buffet.setVendedor(null);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(colegioRepository.findById(schoolId)).thenReturn(Optional.of(colegio));
        when(buffetRepository.findByColegioId(schoolId)).thenReturn(List.of(buffet));

        GetSchoolOverviewRequest request = new GetSchoolOverviewRequest(COGNITO_SUBJECT);

        GetSchoolOverviewResponse response = useCase.execute(request);

        assertEquals(1, response.buffets().size());
        assertNull(response.buffets().get(0).vendedor());
    }
}
