package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "cierres_diarios_buffet",
        uniqueConstraints = @UniqueConstraint(columnNames = {"buffet_id", "fecha"})
)
@Getter
@Setter
@NoArgsConstructor
public class CierreDiarioBuffetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private BuffetEntity buffetEntity;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "pedidos_vencidos", nullable = false)
    private Integer pedidosVencidos = 0;

    @Column(name = "reservas_liberadas", nullable = false)
    private Integer reservasLiberadas = 0;

    @Column(name = "creditos_devueltos", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditosDevueltos = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "cierreDiario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CierreDiarioInventarioSnapshotEntity> inventarioSnapshot =
            new ArrayList<>();

    public DailyBuffetClose toDomain() {
        return new DailyBuffetClose(
                id,
                buffetEntity == null ? null : buffetEntity.getId(),
                fecha,
                pedidosVencidos == null ? 0 : pedidosVencidos,
                reservasLiberadas == null ? 0 : reservasLiberadas,
                creditosDevueltos == null ? BigDecimal.ZERO : creditosDevueltos
        );
    }

    public void setInventarioSnapshotFromDomain(
            List<DailyInventorySnapshot> snapshots
    ) {
        inventarioSnapshot.clear();
        if (snapshots == null) {
            return;
        }

        snapshots.stream()
                .map(CierreDiarioInventarioSnapshotEntity::fromDomain)
                .forEach(this::addInventarioSnapshot);
    }

    public List<DailyInventorySnapshot> inventarioSnapshotToDomain() {
        return inventarioSnapshot.stream()
                .map(CierreDiarioInventarioSnapshotEntity::toDomain)
                .sorted(Comparator.comparing(
                        DailyInventorySnapshot::productName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void addInventarioSnapshot(CierreDiarioInventarioSnapshotEntity snapshot) {
        snapshot.setCierreDiario(this);
        inventarioSnapshot.add(snapshot);
    }
}
