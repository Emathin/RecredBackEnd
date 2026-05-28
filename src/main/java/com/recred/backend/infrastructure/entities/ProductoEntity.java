package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProductoEntity categoria;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    @Column(name = "requiere_preparacion", nullable = false)
    private Boolean requierePreparacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoMedidaEntity> medidas = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_clasificaciones",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_id")
    )
    private Set<ClasificacionTipoEntity> tipos = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_clasificacion_salud",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<ClasificacionSaludEntity> clasificacionesSalud = new HashSet<>();

    public void addMedida(ProductoMedidaEntity medida) {
        medidas.add(medida);
        medida.setProducto(this);
    }

    public void removeMedida(ProductoMedidaEntity medida) {
        medidas.remove(medida);
        medida.setProducto(null);
    }

    public Producto toDomain() {
        Producto domain = new Producto();
        domain.setId(this.id);
        domain.setNombre(this.nombre);
        domain.setDescripcion(this.descripcion);
        domain.setPrecio(this.precio);
        domain.setPeso(this.peso);
        domain.setRequierePreparacion(this.requierePreparacion);
        domain.setActivo(this.activo);
        domain.setCreadoEn(this.creadoEn);
        domain.setActualizadoEn(this.actualizadoEn);
        
        if (this.clasificacionesSalud != null) {
            domain.setClasificacionesSalud(this.clasificacionesSalud.stream().map(c -> {
                ClasificacionSalud cs = new ClasificacionSalud();
                cs.setId(c.getId());
                cs.setDescripcion(c.getDescripcion());
                cs.setActivo(c.getActivo());
                return cs;
            }).collect(Collectors.toSet()));
        }
        
        return domain;
    }
}
