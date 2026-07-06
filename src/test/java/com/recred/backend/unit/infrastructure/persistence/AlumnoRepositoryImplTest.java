package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AlumnoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataGradoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumnoRepositoryImplTest {

    @Mock
    private SpringDataAlumnoRepository springDataRepository;
    @Mock
    private SpringDataUsuarioRepository usuarioRepository;
    @Mock
    private SpringDataGradoRepository gradoRepository;

    private AlumnoRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new AlumnoRepositoryImpl(
                springDataRepository,
                usuarioRepository,
                gradoRepository
        );
    }

    @Test
    void saveShouldMapReferencesAndCollections() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID favoriteId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UsuarioEntity userReference = new UsuarioEntity();
        userReference.setId(userId);
        GradoEntity gradeReference = new GradoEntity();
        gradeReference.setId(gradeId);
        when(springDataRepository.findById(studentId)).thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(userId)).thenReturn(userReference);
        when(gradoRepository.getReferenceById(gradeId)).thenReturn(gradeReference);
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Alumno result = adapter.save(alumno(
                studentId,
                userId,
                gradeId,
                healthId,
                favoriteId,
                blockedId
        ));

        ArgumentCaptor<AlumnoEntity> captor = ArgumentCaptor.forClass(AlumnoEntity.class);
        verify(springDataRepository).save(captor.capture());
        AlumnoEntity saved = captor.getValue();
        assertEquals(studentId, saved.getId());
        assertEquals(true, saved.getActivo());
        assertSame(userReference, saved.getUsuario());
        assertSame(gradeReference, saved.getGradoEntity());
        assertEquals(1, saved.getRestriccionesSalud().size());
        assertEquals(healthId, saved.getRestriccionesSalud().iterator().next().getId());
        assertEquals(1, saved.getProductosFavoritos().size());
        assertEquals(favoriteId, saved.getProductosFavoritos().iterator().next().getId());
        assertEquals(1, saved.getProductosBloqueados().size());
        assertEquals(blockedId, saved.getProductosBloqueados().iterator().next().getId());
        assertEquals(studentId, result.getId());
    }

    @Test
    void saveShouldClearExistingCollectionsWhenDomainCollectionsAreEmpty() {
        UUID studentId = UUID.randomUUID();
        AlumnoEntity existing = new AlumnoEntity();
        existing.setId(studentId);
        existing.setRestriccionesSalud(new java.util.HashSet<>(
                Set.of(healthEntity(UUID.randomUUID()))));
        existing.setProductosFavoritos(new java.util.HashSet<>(
                Set.of(productEntity(UUID.randomUUID()))));
        existing.setProductosBloqueados(new java.util.HashSet<>(
                Set.of(productEntity(UUID.randomUUID()))));
        when(springDataRepository.findById(studentId)).thenReturn(Optional.of(existing));
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(emptyAlumno(studentId));

        assertTrue(existing.getRestriccionesSalud().isEmpty());
        assertTrue(existing.getProductosFavoritos().isEmpty());
        assertTrue(existing.getProductosBloqueados().isEmpty());
    }

    @Test
    void findUserIdShouldDelegateToRepository() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(springDataRepository.findUserId(studentId)).thenReturn(userId);

        UUID result = adapter.findUserID(studentId);

        assertEquals(userId, result);
    }

    private Alumno alumno(
            UUID studentId,
            UUID userId,
            UUID gradeId,
            UUID healthId,
            UUID favoriteId,
            UUID blockedId
    ) {
        Alumno alumno = emptyAlumno(studentId);
        alumno.setActivo(null);
        Usuario user = new Usuario();
        user.setId(userId);
        alumno.setUsuario(user);
        Grado grade = new Grado();
        grade.setId(gradeId);
        alumno.setGrado(grade);
        alumno.setRestriccionesSalud(Set.of(health(healthId)));
        alumno.setProductosFavoritos(Set.of(product(favoriteId), product(null)));
        alumno.setProductosBloqueados(Set.of(product(blockedId), product(null)));
        return alumno;
    }

    private Alumno emptyAlumno(UUID studentId) {
        Alumno alumno = new Alumno();
        alumno.setId(studentId);
        alumno.setActivo(true);
        alumno.setRestriccionesSalud(Set.of());
        alumno.setProductosFavoritos(Set.of());
        alumno.setProductosBloqueados(Set.of());
        return alumno;
    }

    private ClasificacionSalud health(UUID id) {
        ClasificacionSalud health = new ClasificacionSalud();
        health.setId(id);
        health.setDescripcion("Sin TACC");
        health.setActivo(true);
        return health;
    }

    private Producto product(UUID id) {
        Producto product = new Producto();
        product.setId(id);
        return product;
    }

    private ClasificacionSaludEntity healthEntity(UUID id) {
        ClasificacionSaludEntity entity = new ClasificacionSaludEntity();
        entity.setId(id);
        return entity;
    }

    private ProductoEntity productEntity(UUID id) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        return entity;
    }
}
