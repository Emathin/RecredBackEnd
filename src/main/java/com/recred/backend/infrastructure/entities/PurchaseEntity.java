package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.PaymentMethod;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "compras")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class PurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", insertable = false, updatable = false)
    private PurchaseType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private StudentEntity studentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private TutorEntity tutorEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock", nullable = false)
    private PurchaseState purchaseState;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime dateAndTime;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseDetailEntity> details = new ArrayList<>();

    public void addDetalle(PurchaseDetailEntity detalle) {
        details.add(detalle);
        detalle.setPurchase(this);
    }

    public void removeDetalle(PurchaseDetailEntity detalle) {
        details.remove(detalle);
        detalle.setPurchase(null);
    }

    public Purchase toDomain() {
        Purchase domain = new Purchase();
        domain.setId(this.id);
        domain.setTotalAmount(this.totalAmount);
        domain.setStatus(this.purchaseState);
        domain.setPaymentMethod(this.paymentMethod);
        domain.setDate(this.dateAndTime);

        if (this.studentEntity != null) {
            domain.setStudent(this.studentEntity.toDomain());
        }

        if (this.details != null) {
            domain.setItems(this.details.stream()
                    .collect(Collectors.toMap(
                            d -> d.getProductEntity().toDomain(),
                            PurchaseDetailEntity::getAmount
                    )));
        }

        return domain;
    }
}
