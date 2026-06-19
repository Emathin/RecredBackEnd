package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "carritos_favoritos")
@Getter
@Setter
@NoArgsConstructor
public class CarritoFavoritoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorEntity tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumno;

    @OneToMany(mappedBy = "carritoFavorito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarritoFavoritoItemEntity> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public void addItem(CarritoFavoritoItemEntity item) {
        items.add(item);
        item.setCarritoFavorito(this);
    }

    public void removeItem(CarritoFavoritoItemEntity item) {
        items.remove(item);
        item.setCarritoFavorito(null);
    }

    public CarritoFavorito toDomain() {
        CarritoFavorito domain = new CarritoFavorito();
        domain.setId(this.id);
        domain.setNombre(this.nombre);

        if (this.tutor != null) {
            domain.setTutor(this.tutor.toDomain());
        }
        if (this.alumno != null) {
            domain.setAlumno(this.alumno.toDomain());
        }

        if (this.items != null) {
            Map<Producto, Integer> domainItems = new HashMap<>();
            for (CarritoFavoritoItemEntity itemEntity : this.items) {
                if (itemEntity.getProductoEntity() != null) {
                    domainItems.put(
                            itemEntity.getProductoEntity().toDomain(),
                            itemEntity.getCantidad());
                }
            }
            domain.setItems(domainItems);
        }

        return domain;
    }
}
