package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AlumnoRepositoryImpl implements AlumnoRepository {

    private final SpringDataAlumnoRepository springDataRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final SpringDataGradoRepository gradoRepository;

    public AlumnoRepositoryImpl(SpringDataAlumnoRepository springDataRepository,
                                SpringDataUsuarioRepository usuarioRepository,
                                SpringDataGradoRepository gradoRepository) {
        this.springDataRepository = springDataRepository;
        this.usuarioRepository = usuarioRepository;
        this.gradoRepository = gradoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alumno> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(AlumnoEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alumno> findAll() {
        return springDataRepository.findAll().stream()
                .map(AlumnoEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID alumnoId) {
        return springDataRepository.existsById(alumnoId);
    }

    @Override
    public Optional<UUID> findStudentIdByUserId(UUID userId) {
        return springDataRepository.findStudentIdByUserId(userId);
    }

    @Override
    public Optional<UUID> findSchoolIdByStudentId(UUID studentId) {
        return springDataRepository.findSchoolIdByStudentId(studentId);
    }

    @Override
    public UUID findUserID(UUID alumnoId) {
        return springDataRepository.findUserId(alumnoId);
    }

    @Override
    @Transactional
    public Alumno save(Alumno alumno) {
        AlumnoEntity entityExistente = alumno.getId() != null
                ? springDataRepository.findById(alumno.getId()).orElseGet(AlumnoEntity::new)
                : new AlumnoEntity();

        AlumnoEntity entityLista = toEntity(alumno, entityExistente);

        return springDataRepository.save(entityLista).toDomain();
    }

    private AlumnoEntity toEntity(Alumno modelo, AlumnoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setActivo(modelo.getActivo() != null ? modelo.getActivo() : true);

        if (modelo.getUsuario() != null && modelo.getUsuario().getId() != null) {
            UsuarioEntity usuarioEntity = usuarioRepository.getReferenceById(
                    modelo.getUsuario().getId());
            entity.setUsuario(usuarioEntity);
        }

        if (modelo.getGrado() != null && modelo.getGrado().getId() != null) {
            GradoEntity gradoEntity = gradoRepository.getReferenceById(
                    modelo.getGrado().getId());
            entity.setGradoEntity(gradoEntity);
        }

        updateWallet(modelo, entity);

        if (modelo.getRestriccionesSalud() != null && !modelo.getRestriccionesSalud().isEmpty()) {
            Set<ClasificacionSaludEntity> restriccionesEntities = modelo
                    .getRestriccionesSalud().stream()
                    .map(restriccionDominio -> {
                        ClasificacionSaludEntity clasificacionEntity = new
                                ClasificacionSaludEntity();
                        clasificacionEntity.setId(restriccionDominio.getId());
                        clasificacionEntity.setDescripcion(restriccionDominio.getDescripcion());
                        clasificacionEntity.setActivo(restriccionDominio.getActivo());
                        return clasificacionEntity;
                    })
                    .collect(Collectors.toSet());

            entity.setRestriccionesSalud(restriccionesEntities);
        } else {
            if (entity.getRestriccionesSalud() != null) {
                entity.getRestriccionesSalud().clear();
            }
        }

        if (modelo.getProductosFavoritos() != null && !modelo.getProductosFavoritos().isEmpty()) {
            Set<ProductoEntity> productosProxies = modelo.getProductosFavoritos().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosFavoritos(productosProxies);
        } else {
            if (entity.getProductosFavoritos() != null) {
                entity.getProductosFavoritos().clear();
            }
        }

        if (modelo.getProductosBloqueados() != null && !modelo.getProductosBloqueados().isEmpty()) {
            Set<ProductoEntity> bloqueadosProxies = modelo.getProductosBloqueados().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosBloqueados(bloqueadosProxies);
        } else {
            if (entity.getProductosBloqueados() != null) {
                entity.getProductosBloqueados().clear();
            }
        }

        entity.setPuntosRecompensa(modelo.getPuntosRecompensa() != null ?
                modelo.getPuntosRecompensa() : 0);

        return entity;
    }

    private void updateWallet(Alumno modelo, AlumnoEntity entity) {
        if (modelo.getBilletera() == null) {
            return;
        }

        Billetera wallet = modelo.getBilletera();
        BilleteraEntity walletEntity = entity.getBilleteraEntity() != null
                ? entity.getBilleteraEntity()
                : new BilleteraEntity();

        if (walletEntity.getId() == null) {
            walletEntity.setId(wallet.getId());
        }

        walletEntity.setSaldoCreditos(valueOrZero(wallet.getSaldoCreditos()));
        walletEntity.setLimiteCredito(valueOrZero(wallet.getLimiteCredito()));
        walletEntity.setSaldoDeudaInterno(valueOrZero(wallet.getSaldoDeudaInterno()));
        walletEntity.setCreditoHabilitado(Boolean.TRUE.equals(wallet.getCreditoHabilitado()));
        walletEntity.setAlumnoEntity(entity);
        entity.setBilleteraEntity(walletEntity);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
