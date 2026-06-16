package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryImplTest {

    @Mock
    private SpringDataUsuarioRepository springDataUsuarioRepository;

    private UsuarioRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UsuarioRepositoryImpl(springDataUsuarioRepository);
    }

    @Test
    void saveShouldMapDomainUserWithRelationsToEntityAndBack() {
        Usuario usuario = user();
        Colegio colegio = new Colegio();
        colegio.setId(UUID.randomUUID());
        usuario.setColegio(colegio);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        usuario.setAlumno(alumno);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        usuario.setTutor(tutor);
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(UUID.randomUUID());
        Buffet buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        kiosquero.setBuffet(buffet);
        usuario.setKiosquero(kiosquero);
        when(springDataUsuarioRepository.save(any(UsuarioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = repository.save(usuario);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(springDataUsuarioRepository).save(captor.capture());
        UsuarioEntity saved = captor.getValue();
        assertEquals(usuario.getId(), saved.getId());
        assertEquals(usuario.getIdCognito(), saved.getIdCognito());
        assertEquals(colegio.getId(), saved.getColegioEntity().getId());
        assertEquals(alumno.getId(), saved.getAlumnoEntity().getId());
        assertEquals(tutor.getId(), saved.getTutor().getId());
        assertEquals(kiosquero.getId(), saved.getKiosquero().getId());
        assertEquals(buffet.getId(), saved.getKiosquero().getBuffetEntity().getId());
        assertEquals(usuario.getId(), result.getId());
        assertEquals(usuario.getEmail(), result.getEmail());
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        UUID id = UUID.randomUUID();
        UsuarioEntity entity = userEntity(id);
        when(springDataUsuarioRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Usuario> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.orElseThrow().getId());
    }

    @Test
    void findByIdCognitoShouldMapEntityWhenPresent() {
        UsuarioEntity entity = userEntity(UUID.randomUUID());
        when(springDataUsuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(entity));

        Optional<Usuario> result = repository.findByIdCognito("sub");

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.orElseThrow().getId());
    }

    @Test
    void findByEmailShouldMapEntityWhenPresent() {
        UsuarioEntity entity = userEntity(UUID.randomUUID());
        when(springDataUsuarioRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(entity));

        Optional<Usuario> result = repository.findByEmail("user@example.com");

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.orElseThrow().getId());
    }

    private Usuario user() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setIdCognito("sub");
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setEmail("user@example.com");
        usuario.setTelefono("112233");
        usuario.setDni("12345678");
        usuario.setRol(RolUsuario.PADRE);
        usuario.setAutenticadoConCognito(true);
        usuario.setActivo(true);
        usuario.setCreadoEn(LocalDateTime.of(2026, 6, 15, 10, 0));
        usuario.setActualizadoEn(LocalDateTime.of(2026, 6, 15, 11, 0));
        return usuario;
    }

    private UsuarioEntity userEntity(UUID id) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(id);
        entity.setIdCognito("sub");
        entity.setNombre("Juan");
        entity.setApellido("Perez");
        entity.setEmail("user@example.com");
        entity.setTelefono("112233");
        entity.setDni("12345678");
        entity.setRol(RolUsuario.PADRE);
        entity.setAutenticadoConCognito(true);
        entity.setActivo(true);
        return entity;
    }
}
