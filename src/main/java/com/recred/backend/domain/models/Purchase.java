package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    private UUID id;
    private Alumno student;
    private BigDecimal totalAmount;
    private EstadoCompra status;
    private MedioPago paymentMethod;
    private LocalDateTime date;
    private UUID buyerId;
    private BuyerType buyerType;
    private UUID buffetId;
    private TipoCompra tipo = TipoCompra.PRESENCIAL;
    private String withdrawalCode;
    private UUID pickupSlotId;
    private String pickupSlotDescription;
    private LocalTime pickupSlotStart;
    private LocalTime pickupSlotEnd;
    private LocalDate pickupDate;
    private java.time.LocalTime pickupSlotStartTime;
    private EstadoRetiro withdrawalStatus;
    private UUID suggestionId;
    private UUID tutorId;

    private Map<Producto, Integer> items = new HashMap<>();

    public Purchase(Alumno student, Map<Producto, Integer> cart, MedioPago paymentMethod,
                    UUID buyerId, BuyerType buyerType) {
        this.student = student;
        this.items = cart;
        this.paymentMethod = paymentMethod;
        this.status = initialStatus(cart);
        this.date = LocalDateTime.now();
        this.totalAmount = calculateTotal(cart);
        this.buyerId = buyerId;
        this.buyerType = buyerType;
        this.tipo = TipoCompra.PRESENCIAL;
    }

    public Purchase(Alumno student, Map<Producto, Integer> cart, MedioPago paymentMethod,
                    UUID buyerId, BuyerType buyerType, UUID buffetId) {
        this(student, cart, paymentMethod, buyerId, buyerType);
        this.buffetId = buffetId;
    }

    public static Purchase advance(Alumno student, Map<Producto, Integer> cart,
                                   MedioPago paymentMethod, UUID buyerId,
                                   BuyerType buyerType, UUID buffetId,
                                   UUID pickupSlotId, LocalDate pickupDate) {
        Purchase purchase = new Purchase(student, cart, paymentMethod, buyerId, buyerType,
                buffetId);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupDate(pickupDate);
        return purchase;
    }

    public void changeStatusTo(EstadoCompra nextStatus) {
        if (isInvalidTransition(nextStatus)) {
            InvalidPurchaseStatusTransitionException exception =
                    new InvalidPurchaseStatusTransitionException(this.status, nextStatus);
            throw exception;
        }

        this.status = nextStatus;
        if (this.tipo == TipoCompra.ANTICIPADA) {
            this.withdrawalStatus = switch (nextStatus) {
                case PENDIENTE -> EstadoRetiro.PROGRAMADO;
                case EN_PREPARACION -> EstadoRetiro.EN_PREPARACION;
                case LISTO -> EstadoRetiro.LISTO;
                case ENTREGADO -> EstadoRetiro.RETIRADO;
                case CANCELADO, RECHAZADO, VENCIDO -> EstadoRetiro.CANCELADO;
                case NO_RETIRADO -> EstadoRetiro.NO_RETIRADO;
            };
        }
    }

    public void changeTrackingStatusTo(EstadoCompra nextStatus) {
        if (nextStatus == EstadoCompra.CANCELADO) {
            throw new InvalidPurchaseStatusTransitionException(this.status, nextStatus);
        }

        changeStatusTo(nextStatus);
    }

    public void cancel() {
        if (this.status == EstadoCompra.CANCELADO) {
            throw new IllegalStateException("Purchase is already cancelled.");
        }
        
        if (this.tipo == TipoCompra.PRESENCIAL && this.status != EstadoCompra.PENDIENTE) {
            throw new InvalidPurchaseStatusTransitionException(this.status, EstadoCompra.CANCELADO);
        }

        if (this.status != EstadoCompra.PENDIENTE && this.status != EstadoCompra.EN_PREPARACION) {
            throw new InvalidPurchaseStatusTransitionException(this.status, EstadoCompra.CANCELADO);
        }
        
        this.status = EstadoCompra.CANCELADO;
        if (this.tipo == TipoCompra.ANTICIPADA) {
            this.withdrawalStatus = EstadoRetiro.CANCELADO;
        }
    }

    public void validateCancellationWindow(FranjaHoraria pickupSlot, java.time.LocalDateTime now) {
        if (this.tipo != TipoCompra.ANTICIPADA ||
                this.pickupSlotId == null || this.pickupDate == null) {
            return;
        }
        
        if (pickupSlot == null) {
            throw new IllegalArgumentException("Time slot is required for advance " +
                    "purchase cancellation validation");
        }

        java.time.LocalDateTime startDateTime = java.time.LocalDateTime.of(
                this.pickupDate, pickupSlot.getHoraInicio());
        if (!now.isBefore(startDateTime.minusHours(1))) {
            throw new IllegalArgumentException(
                    "Cannot cancel an order less than an hour before the pickup time starts");
        }
    }

    public boolean hasStatus(EstadoCompra status) {
        return this.status == status;
    }

    public boolean hasType(TipoCompra type) {
        return this.tipo == type;
    }

    public boolean isDelivered() {
        return hasStatus(EstadoCompra.ENTREGADO);
    }

    public boolean isAdvancePurchase() {
        return hasType(TipoCompra.ANTICIPADA);
    }

    public boolean shouldConsumeReservationWhenChangingTo(EstadoCompra requestedStatus) {
        return isAdvancePurchase() && requestedStatus == EstadoCompra.ENTREGADO;
    }

    public boolean shouldReleaseReservationWhenChangingFrom(
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus
    ) {
        return isAdvancePurchase()
                && hasActiveReservation(previousStatus)
                && (requestedStatus == EstadoCompra.RECHAZADO
                        || requestedStatus == EstadoCompra.VENCIDO);
    }

    public boolean shouldRefundWhenChangingTo(EstadoCompra requestedStatus) {
        return isAdvancePurchase() && requestedStatus == EstadoCompra.RECHAZADO;
    }

    public boolean shouldPublishExpirationEventFor(EstadoCompra requestedStatus) {
        return requestedStatus == EstadoCompra.VENCIDO;
    }

    public boolean refundsCredits() {
        return hasStatus(EstadoCompra.CANCELADO) || hasStatus(EstadoCompra.RECHAZADO);
    }

    public boolean happenedBetween(LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }

        LocalDate purchaseDate = date.toLocalDate();
        return !purchaseDate.isBefore(from) && !purchaseDate.isAfter(to);
    }

    public boolean hasDate() {
        return date != null;
    }

    public LocalDate purchaseDate() {
        return date.toLocalDate();
    }

    public BigDecimal totalAmountOrZero() {
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public boolean hasPickupSlotDescription() {
        return pickupSlotDescription != null && !pickupSlotDescription.isBlank();
    }

    public boolean hasStudentId() {
        return studentIdOrNull() != null;
    }

    public UUID studentIdOrNull() {
        return student == null ? null : student.getId();
    }

    public UUID requireStudentId() {
        if (!hasStudentId()) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }

        return studentIdOrNull();
    }

    public String statusName() {
        return status == null ? null : status.name();
    }

    public String statusLabel() {
        return status == null ? null : status.getLabel();
    }

    public String paymentMethodName() {
        return paymentMethod == null ? null : paymentMethod.name();
    }

    public String typeName() {
        return tipo == null ? null : tipo.name();
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> productPrice(entry.getKey())
                        .multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productPrice(Producto product) {
        return product.getPrecio() != null ? product.getPrecio() : BigDecimal.ZERO;
    }

    private EstadoCompra initialStatus(Map<Producto, Integer> cart) {
        return requiresPreparation(cart) ? EstadoCompra.PENDIENTE : EstadoCompra.ENTREGADO;
    }

    private boolean requiresPreparation(Map<Producto, Integer> cart) {
        return cart.keySet().stream().anyMatch(this::requiresPreparation);
    }

    private boolean requiresPreparation(Producto product) {
        return Boolean.TRUE.equals(product.getRequierePreparacion());
    }

    private boolean hasActiveReservation(EstadoCompra status) {
        return status == EstadoCompra.PENDIENTE
                || status == EstadoCompra.EN_PREPARACION
                || status == EstadoCompra.LISTO;
    }

    private boolean isInvalidTransition(EstadoCompra nextStatus) {
        return this.status == null || nextStatus == null || !isAllowedTransition(nextStatus);
    }

    private boolean isAllowedTransition(EstadoCompra nextStatus) {
        return switch (this.status) {
            case PENDIENTE -> nextStatus == EstadoCompra.EN_PREPARACION
                    || nextStatus == EstadoCompra.CANCELADO
                    || nextStatus == EstadoCompra.RECHAZADO
                    || nextStatus == EstadoCompra.VENCIDO;
            case EN_PREPARACION -> nextStatus == EstadoCompra.LISTO
                    || nextStatus == EstadoCompra.CANCELADO
                    || nextStatus == EstadoCompra.RECHAZADO
                    || nextStatus == EstadoCompra.VENCIDO;
            case LISTO -> nextStatus == EstadoCompra.ENTREGADO
                    || nextStatus == EstadoCompra.RECHAZADO
                    || nextStatus == EstadoCompra.VENCIDO
                    || nextStatus == EstadoCompra.NO_RETIRADO;
            case ENTREGADO, CANCELADO, RECHAZADO, VENCIDO, NO_RETIRADO -> false;
        };
    }
}
