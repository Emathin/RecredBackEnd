package com.recred.backend.integration.persistence;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private AlumnoRepositoryImpl alumnoRepositoryImpl;

    @Captor
    private ArgumentCaptor<AlumnoEntity> entityCaptor;

    private UUID alumnoId;
    private UUID clasificacionId;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
    }

    @Test
    void save_DeberiaMapearADominioYLlamarAlSpringData() {
        Alumno alumnoDominio = new Alumno();
        alumnoDominio.setId(alumnoId);

        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescripcion("Celíaco");
        alumnoDominio.agregarRestriccionSalud(clasificacion);

        Producto productoFav = new Producto();
        productoFav.setId(UUID.randomUUID());
        alumnoDominio.addFavoriteProduct(productoFav);

        Producto productoBloq = new Producto();
        productoBloq.setId(UUID.randomUUID());
        alumnoDominio.bloquearProducto(productoBloq);

        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alumnoRepositoryImpl.save(alumnoDominio);

        verify(springDataRepository).save(entityCaptor.capture());
        AlumnoEntity entidadAtrapada = entityCaptor.getValue();

        assertNotNull(entidadAtrapada, "La entidad generada no debe ser nula");
        assertEquals(alumnoId, entidadAtrapada.getId());

        assertNotNull(entidadAtrapada.getRestriccionesSalud(), "La lista de entidades no debe ser nula");
        assertEquals(1, entidadAtrapada.getRestriccionesSalud().size(), "Debe haber mapeado 1 restricción");
        assertEquals(clasificacionId, entidadAtrapada.getRestriccionesSalud().iterator().next().getId(), "El ID de la restricción debe coincidir");

        assertNotNull(entidadAtrapada.getProductosFavoritos(), "La lista de favoritos no debe ser nula");
        assertEquals(1, entidadAtrapada.getProductosFavoritos().size(), "Debe haber mapeado 1 favorito");
        assertEquals(productoFav.getId(), entidadAtrapada.getProductosFavoritos().iterator().next().getId());

        assertNotNull(entidadAtrapada.getProductosBloqueados(), "La lista de bloqueados no debe ser nula");
        assertEquals(1, entidadAtrapada.getProductosBloqueados().size(), "Debe haber mapeado 1 bloqueado");
        assertEquals(productoBloq.getId(), entidadAtrapada.getProductosBloqueados().iterator().next().getId());
    }

    @Test
    void findById_CuandoExiste_DeberiaMapearADominioYRetornar() {
        AlumnoEntity entidadFalsa = new AlumnoEntity();
        entidadFalsa.setId(alumnoId);

        ClasificacionSaludEntity clasificacionEntity = new ClasificacionSaludEntity();
        clasificacionEntity.setId(clasificacionId);
        clasificacionEntity.setDescripcion("Diabético");
        entidadFalsa.setRestriccionesSalud(Set.of(clasificacionEntity));

        UUID productoId = UUID.randomUUID();
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(productoId);
        productoEntity.setNombre("Galletitas");
        entidadFalsa.setProductosFavoritos(Set.of(productoEntity));

        UUID productoBloqId = UUID.randomUUID();
        ProductoEntity productoBloqEntity = new ProductoEntity();
        productoBloqEntity.setId(productoBloqId);
        productoBloqEntity.setNombre("Alfajor");
        entidadFalsa.setProductosBloqueados(Set.of(productoBloqEntity));

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(entidadFalsa));

        Optional<Alumno> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isPresent(), "El alumno debe estar presente");
        Alumno alumnoMapeado = resultado.get();

        assertEquals(alumnoId, alumnoMapeado.getId());
        assertNotNull(alumnoMapeado.getRestriccionesSalud());
        assertEquals(1, alumnoMapeado.getRestriccionesSalud().size());

        ClasificacionSalud restriccionMapeada = alumnoMapeado.getRestriccionesSalud().iterator().next();
        assertEquals(clasificacionId, restriccionMapeada.getId());
        assertEquals("Diabético", restriccionMapeada.getDescripcion());

        assertNotNull(alumnoMapeado.getProductosFavoritos());
        assertEquals(1, alumnoMapeado.getProductosFavoritos().size());
        assertEquals(productoId, alumnoMapeado.getProductosFavoritos().iterator().next().getId());

        assertNotNull(alumnoMapeado.getProductosBloqueados());
        assertEquals(1, alumnoMapeado.getProductosBloqueados().size());
        assertEquals(productoBloqId, alumnoMapeado.getProductosBloqueados().iterator().next().getId());

        verify(springDataRepository).findById(alumnoId);
    }

    @Test
    void findById_CuandoNoExiste_DeberiaRetornarVacio() {
        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Optional<Alumno> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isEmpty());
        verify(springDataRepository).findById(alumnoId);
    }

    @Test
    void findStudentIdByUserId_CuandoExiste_DeberiaRetornarIdAlumno() {
        UUID usuarioId = UUID.randomUUID();
        when(springDataRepository.findStudentIdByUserId(usuarioId))
                .thenReturn(Optional.of(alumnoId));

        Optional<UUID> resultado = alumnoRepositoryImpl.findStudentIdByUserId(usuarioId);

        assertEquals(Optional.of(alumnoId), resultado);
        verify(springDataRepository).findStudentIdByUserId(usuarioId);
    }

    @Test
    void findAllMapsEntitiesToDomain() {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(alumnoId);
        when(springDataRepository.findAll()).thenReturn(java.util.List.of(entity));

        java.util.List<Alumno> result = alumnoRepositoryImpl.findAll();

        assertEquals(1, result.size());
        assertEquals(alumnoId, result.get(0).getId());
    }

    @Test
    void existsByIdDelegatesToSpringDataRepository() {
        when(springDataRepository.existsById(alumnoId)).thenReturn(true);

        boolean result = alumnoRepositoryImpl.existsById(alumnoId);

        assertTrue(result);
        verify(springDataRepository).existsById(alumnoId);
    }

    @Test
    void findUserIDDelegatesToSpringDataRepository() {
        UUID userId = UUID.randomUUID();
        when(springDataRepository.findUserId(alumnoId)).thenReturn(userId);

        UUID result = alumnoRepositoryImpl.findUserID(alumnoId);

        assertEquals(userId, result);
        verify(springDataRepository).findUserId(alumnoId);
    }

    @Test
    void saveWithoutIdCreatesNewEntityWithDefaultActiveStatus() {
        Alumno alumno = new Alumno();
        alumno.setActivo(null);
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alumnoRepositoryImpl.save(alumno);

        verify(springDataRepository).save(entityCaptor.capture());
        assertTrue(entityCaptor.getValue().getActivo());
        verify(springDataRepository, never()).findById(any());
    }

    @Test
    void saveMapsUserAndGradeReferences() {
        UUID userId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        Grado grado = new Grado();
        grado.setId(gradeId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setUsuario(usuario);
        alumno.setGrado(grado);
        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setId(userId);
        GradoEntity gradoEntity = new GradoEntity();
        gradoEntity.setId(gradeId);

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(new AlumnoEntity()));
        when(usuarioRepository.getReferenceById(userId)).thenReturn(usuarioEntity);
        when(gradoRepository.getReferenceById(gradeId)).thenReturn(gradoEntity);
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alumnoRepositoryImpl.save(alumno);

        verify(springDataRepository).save(entityCaptor.capture());
        AlumnoEntity entity = entityCaptor.getValue();
        assertEquals(usuarioEntity, entity.getUsuario());
        assertEquals(gradoEntity, entity.getGradoEntity());
    }

    @Test
    void saveClearsExistingCollectionsWhenDomainCollectionsAreEmpty() {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setRestriccionesSalud(Set.of());
        alumno.setProductosFavoritos(Set.of());
        alumno.setProductosBloqueados(Set.of());
        AlumnoEntity entity = new AlumnoEntity();
        entity.getRestriccionesSalud().add(new ClasificacionSaludEntity());
        entity.getProductosFavoritos().add(new ProductoEntity());
        entity.getProductosBloqueados().add(new ProductoEntity());

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(entity));
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alumnoRepositoryImpl.save(alumno);

        assertTrue(entity.getRestriccionesSalud().isEmpty());
        assertTrue(entity.getProductosFavoritos().isEmpty());
        assertTrue(entity.getProductosBloqueados().isEmpty());
    }

    @Test
    void saveFiltersFavoriteAndBlockedProductsWithoutId() {
        UUID favoriteId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        Producto favoriteWithoutId = new Producto();
        Producto favoriteWithId = new Producto();
        favoriteWithId.setId(favoriteId);
        Producto blockedWithoutId = new Producto();
        Producto blockedWithId = new Producto();
        blockedWithId.setId(blockedId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setProductosFavoritos(Set.of(favoriteWithoutId, favoriteWithId));
        alumno.setProductosBloqueados(Set.of(blockedWithoutId, blockedWithId));

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(new AlumnoEntity()));
        when(springDataRepository.save(any(AlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        alumnoRepositoryImpl.save(alumno);

        verify(springDataRepository).save(entityCaptor.capture());
        AlumnoEntity entity = entityCaptor.getValue();
        assertEquals(1, entity.getProductosFavoritos().size());
        assertEquals(favoriteId, entity.getProductosFavoritos().iterator().next().getId());
        assertEquals(1, entity.getProductosBloqueados().size());
        assertEquals(blockedId, entity.getProductosBloqueados().iterator().next().getId());
    }
}
