package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AlumnoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataCategoryRepository;
import com.recred.backend.infrastructure.persistence.SpringDataClasificacionSaludRepository;
import com.recred.backend.infrastructure.persistence.SpringDataProductRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class AlumnoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private SpringDataAlumnoRepository springDataRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    private SpringDataClasificacionSaludRepository clasificacionSaludRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataProductRepository productoRepository;

    @Autowired
    private AlumnoRepositoryImpl alumnoRepositoryImpl;

    private UUID usuarioId;
    private UUID clasificacionId;
    private UUID productoFavId;
    private UUID productoBloqId;

    @BeforeEach
    void setUp() {
        UsuarioEntity usuarioEntity = generateTestUsuario();
        usuarioId = usuarioEntity.getId();

        ClasificacionSaludEntity clasificacion = generateClasificacionSaludEntity();
        clasificacionId = clasificacion.getId();

        CategoriaProductoEntity categoria = createAndSaveCategoriaProductoEntity();

        ProductoEntity productoFav = createProductFav(categoria);
        productoFavId = productoFav.getId();

        ProductoEntity productoBloq = createProductBloq(categoria);
        productoBloqId = productoBloq.getId();
    }

    @Test
    void save_DeberiaGuardarEnBaseDeDatosRealYGenerarId() {

        Alumno domainAlumno = prepareDomainModelAlumno();

        Alumno savedAlumno = alumnoRepositoryImpl.save(domainAlumno);

        assertNotNull(savedAlumno.getId(), "PostgreSQL debería haber generado el UUID del Alumno");

        Optional<AlumnoEntity> alumnoInDB = springDataRepository.findById(savedAlumno.getId());
        assertTrue(alumnoInDB.isPresent(), "El alumno debe existir realmente en PostgreSQL");
        
        AlumnoEntity entidadReal = alumnoInDB.get();

        assertEquals(usuarioId, entidadReal.getUsuario().getId(), "El foreign key a Usuario debe coincidir");
        assertEquals(1, entidadReal.getRestriccionesSalud().size(), "Debe haber insertado la relación ManyToMany de restricciones");
        assertEquals(1, entidadReal.getProductosFavoritos().size(), "Debe haber insertado la relación ManyToMany de favoritos");
        assertEquals(1, entidadReal.getProductosBloqueados().size(), "Debe haber insertado la relación ManyToMany de bloqueados");
    }

    @Test
    void findById_DeberiaMapearADominioDesdeBdReal() {

        AlumnoEntity entity = springDataRepository.saveAndFlush(createAlumnoEntityWithHealthClasifyAndFavProduct());

        Optional<Alumno> foundStudent = alumnoRepositoryImpl.findById(entity.getId());

        assertTrue(foundStudent.isPresent());
        Alumno mappedStudent = foundStudent.get();
        assertEquals(entity.getId(), mappedStudent.getId());
        assertEquals(1, mappedStudent.getRestriccionesSalud().size());
        assertEquals(clasificacionId, mappedStudent.getRestriccionesSalud().iterator().next().getId());
    }

    @Test
    void findStudentIdByUserId_DeberiaHacerSelectPorUserIdYRetornarIdAlumno() {

        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createAlumnoEntityWithHealthClasifyAndFavProduct());

        Optional<UUID> resultado = alumnoRepositoryImpl.findStudentIdByUserId(usuarioId);

        assertTrue(resultado.isPresent());
        assertEquals(studentEntity.getId(), resultado.get());
    }

    @Test
    void findAll_DeberiaTraerTodosDeLaBd() {
        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createAlumnoEntityWithHealthClasifyAndFavProduct());

        List<Alumno> result = alumnoRepositoryImpl.findAll();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(a -> a.getId().equals(studentEntity.getId())));
    }

    @Test
    void existsById_DeberiaEjecutarExistsQueryEnPostgreSQL() {
        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createAlumnoEntityWithHealthClasifyAndFavProduct());

        boolean result = alumnoRepositoryImpl.existsById(studentEntity.getId());

        assertTrue(result);
    }

    @NotNull
    private ProductoEntity createProductBloq(CategoriaProductoEntity categoria) {
        ProductoEntity productoBloq = new ProductoEntity();
        productoBloq.setNombre("Caramelos");
        productoBloq.setDescripcion("Golosina con azúcar");
        productoBloq.setPrecio(BigDecimal.valueOf(50));
        productoBloq.setCategoria(categoria);
        productoBloq.setRequierePreparacion(false);
        productoBloq = productoRepository.saveAndFlush(productoBloq);
        return productoBloq;
    }

    @NotNull
    private ProductoEntity createProductFav(CategoriaProductoEntity categoria) {
        ProductoEntity productoFav = new ProductoEntity();
        productoFav.setNombre("Alfajor de Arroz");
        productoFav.setDescripcion("Alfajor sin TACC");
        productoFav.setPrecio(BigDecimal.valueOf(100));
        productoFav.setCategoria(categoria);
        productoFav.setRequierePreparacion(false);
        productoFav = productoRepository.saveAndFlush(productoFav);
        return productoFav;
    }

    @NotNull
    private CategoriaProductoEntity createAndSaveCategoriaProductoEntity() {
        CategoriaProductoEntity categoria = new CategoriaProductoEntity();
        categoria.setDescripcion("Snacks " + UUID.randomUUID());
        categoria = categoryRepository.saveAndFlush(categoria);
        return categoria;
    }

    @NotNull
    private ClasificacionSaludEntity generateClasificacionSaludEntity() {
        ClasificacionSaludEntity clasificacion = new ClasificacionSaludEntity();
        clasificacion.setDescripcion("Celíaco " + UUID.randomUUID());
        clasificacion = clasificacionSaludRepository.saveAndFlush(clasificacion);
        return clasificacion;
    }

    @NotNull
    private UsuarioEntity generateTestUsuario() {
        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setIdCognito("cognito_sub_" + UUID.randomUUID());
        usuarioEntity.setNombre("Juan");
        usuarioEntity.setApellido("Perez");
        usuarioEntity.setEmail("juan" + UUID.randomUUID() + "@test.com");
        usuarioEntity.setRol(RolUsuario.ALUMNO);
        usuarioEntity.setAutenticadoConCognito(true);
        usuarioEntity = usuarioRepository.saveAndFlush(usuarioEntity);
        return usuarioEntity;
    }

    @NotNull
    private Alumno prepareDomainModelAlumno() {
        Alumno alumnoDominio = new Alumno();

        Usuario usuarioDominio = new Usuario();
        usuarioDominio.setId(usuarioId);
        alumnoDominio.setUsuario(usuarioDominio);
        alumnoDominio.setActivo(true);

        ClasificacionSalud clasifDominio = new ClasificacionSalud();
        clasifDominio.setId(clasificacionId);
        alumnoDominio.agregarRestriccionSalud(clasifDominio);

        Producto prodFavDominio = new Producto();
        prodFavDominio.setId(productoFavId);
        alumnoDominio.agregarProductoFavorito(prodFavDominio);

        Producto prodBloqDominio = new Producto();
        prodBloqDominio.setId(productoBloqId);
        alumnoDominio.bloquearProducto(prodBloqDominio);
        return alumnoDominio;
    }

    @NotNull
    private AlumnoEntity createAlumnoEntityWithHealthClasifyAndFavProduct() {
        AlumnoEntity entidad = new AlumnoEntity();
        entidad.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        entidad.getRestriccionesSalud().add(clasificacionSaludRepository.getReferenceById(clasificacionId));
        entidad.getProductosFavoritos().add(productoRepository.getReferenceById(productoFavId));
        return entidad;
    }

}
