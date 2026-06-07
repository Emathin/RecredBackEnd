package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoRepositoryImplTest {

    @Mock
    private SpringDataAlumnoRepository springDataRepository;

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
        alumnoDominio.agregarProductoFavorito(productoFav);

        Producto productoBloq = new Producto();
        productoBloq.setId(UUID.randomUUID());
        alumnoDominio.bloquearProducto(productoBloq);

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
}
