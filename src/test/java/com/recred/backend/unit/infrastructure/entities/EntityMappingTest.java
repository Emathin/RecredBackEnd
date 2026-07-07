package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityMappingTest {

    @Test
    void nivelEntityShouldMapToDomain() {
        UUID id = UUID.randomUUID();
        NivelEntity entity = new NivelEntity();
        entity.setId(id);
        entity.setDescripcion("Primario");
        entity.setActivo(true);

        Nivel result = entity.toDomain();

        assertEquals(id, result.getId());
        assertEquals("Primario", result.getDescripcion());
        assertEquals(true, result.getActivo());
    }

    @Test
    void clasificacionTipoEntityShouldMapToDomain() {
        UUID id = UUID.randomUUID();
        ClasificacionTipoEntity entity = new ClasificacionTipoEntity();
        entity.setId(id);
        entity.setDescripcion("Frio");
        entity.setActivo(true);

        ClasificacionTipo result = entity.toDomain();

        assertEquals(id, result.getId());
        assertEquals("Frio", result.getDescripcion());
        assertEquals(true, result.getActivo());
    }

    @Test
    void buffetEntityShouldMapToDomainWithSeller() {
        UUID buffetId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        LocalDate expiration = LocalDate.of(2026, 12, 31);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 11, 0);
        KiosqueroEntity seller = new KiosqueroEntity();
        seller.setId(sellerId);
        seller.setCuit("20-12345678-9");
        BuffetEntity entity = new BuffetEntity();
        entity.setId(buffetId);
        entity.setNombre("Buffet central");
        entity.setFechaVencimientoHabilitacion(expiration);
        entity.setActivo(true);
        entity.setCreadoEn(createdAt);
        entity.setActualizadoEn(updatedAt);
        entity.setVendedor(seller);

        Buffet result = entity.toDomain();

        assertEquals(buffetId, result.getId());
        assertEquals("Buffet central", result.getNombre());
        assertEquals(expiration, result.getFechaVencimientoHabilitacion());
        assertEquals(true, result.getActivo());
        assertEquals(createdAt, result.getCreadoEn());
        assertEquals(updatedAt, result.getActualizadoEn());
        assertEquals(sellerId, result.getVendedor().getId());
        assertEquals("20-12345678-9", result.getVendedor().getCuit());
    }

    @Test
    void nestedSchoolGradeAndBuffetEntitiesShouldMapToDomain() {
        NivelEntity nivel = new NivelEntity();
        nivel.setId(UUID.randomUUID());
        nivel.setDescripcion("Primario");
        ColegioEntity colegio = new ColegioEntity();
        colegio.setId(UUID.randomUUID());
        colegio.setNombre("Colegio");
        colegio.setNivelEntity(nivel);
        GradoEntity grado = new GradoEntity();
        grado.setId(UUID.randomUUID());
        grado.setAnio("1");
        grado.setDivision("A");
        grado.setColegioEntity(colegio);
        grado.setNivelEntity(nivel);
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre("Buffet");
        buffet.setColegioEntity(colegio);

        assertEquals("Primario", colegio.toDomain().getNivel().getDescripcion());
        assertEquals(colegio.getId(), grado.toDomain().getColegio().getId());
        assertEquals("Primario", grado.toDomain().getNivel().getDescripcion());
        assertEquals(colegio.getId(), buffet.toDomain().getColegio().getId());
    }

    @Test
    void billeteraEntityFromDomainShouldReturnNullWhenDomainIsNull() {
        assertNull(BilleteraEntity.fromDomain(null));
    }

    @Test
    void billeteraEntityFromDomainShouldMapFields() {
        Billetera billetera = new Billetera();
        billetera.setId(UUID.randomUUID());
        billetera.setSaldoCreditos(BigDecimal.TEN);
        billetera.setCreditoHabilitado(true);

        BilleteraEntity entity = BilleteraEntity.fromDomain(billetera);

        assertEquals(billetera.getId(), entity.getId());
        assertEquals(BigDecimal.TEN, entity.getSaldoCreditos());
        assertEquals(true, entity.getCreditoHabilitado());
    }

    @Test
    void dailyBuffetCloseEntityShouldDefaultNullValuesAndHandleNullSnapshots() {
        CierreDiarioBuffetEntity entity = new CierreDiarioBuffetEntity();
        entity.setId(UUID.randomUUID());
        entity.setFecha(LocalDate.of(2026, 6, 16));
        entity.setPedidosVencidos(null);
        entity.setReservasLiberadas(null);
        entity.setCreditosDevueltos(null);

        var result = entity.toDomain();
        entity.setInventarioSnapshotFromDomain(null);

        assertNull(result.buffetId());
        assertEquals(0, result.expiredPurchases());
        assertEquals(0, result.releasedReservations());
        assertEquals(BigDecimal.ZERO, result.refundedCredits());
        assertEquals(List.of(), entity.inventarioSnapshotToDomain());
    }

    @Test
    void compraEntityShouldAddAndRemoveDetails() {
        CompraEntity compra = new TestCompraEntity();
        CompraDetalleEntity detalle = new CompraDetalleEntity();

        compra.addDetalle(detalle);
        compra.removeDetalle(detalle);

        assertFalse(compra.getDetalles().contains(detalle));
        assertNull(detalle.getCompra());
    }

    @Test
    void compraEntityUnknownSubtypeShouldMapStoredType() {
        UUID id = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 15, 10, 0);
        CompraEntity compra = new TestCompraEntity();
        compra.setId(id);
        compra.setTipo(TipoCompra.ANTICIPADA);
        compra.setEstado(EstadoCompra.PENDIENTE);
        compra.setMedioPago(MedioPago.CREDITOS);
        compra.setMontoTotal(new BigDecimal("100.00"));
        compra.setFechaHora(dateTime);

        Purchase result = compra.toDomain();

        assertEquals(id, result.getId());
        assertEquals(TipoCompra.ANTICIPADA, result.getTipo());
        assertEquals(EstadoCompra.PENDIENTE, result.getStatus());
        assertEquals(MedioPago.CREDITOS, result.getPaymentMethod());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());
        assertEquals(dateTime, result.getDate());
    }

    @Test
    void productoEntityShouldAddAndRemoveMeasure() {
        ProductoEntity producto = new ProductoEntity();
        ProductoMedidaEntity medida = new ProductoMedidaEntity();

        producto.addMedida(medida);
        producto.removeMedida(medida);

        assertFalse(producto.getMedidas().contains(medida));
        assertNull(medida.getProducto());
    }

    private static class TestCompraEntity extends CompraEntity {
    }
}
