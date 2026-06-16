package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.WalletCategorySpending;
import com.recred.backend.domain.models.WalletHealthSpending;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.domain.services.CognitoUserProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebDtoMappingTest {

    @Test
    void alumnoAndProductoDtosExposeFieldsAndUpdates() {
        UUID clasificacionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ClasificacionSaludDTO clasificacion =
                new ClasificacionSaludDTO(clasificacionId, "Sin gluten");
        AlumnoDTO alumno = new AlumnoDTO(alumnoId, Set.of(clasificacion));
        ClasificacionSaludDTO nuevaClasificacion =
                new ClasificacionSaludDTO(UUID.randomUUID(), "Vegano");
        ProductoDTO producto = new ProductoDTO(
                productId,
                "Yogur",
                Set.of(clasificacion)
        );

        alumno.actualizarRestricciones(Set.of(nuevaClasificacion));

        assertEquals(alumnoId, alumno.getId());
        assertEquals(Set.of(nuevaClasificacion), alumno.getRestriccionesSalud());
        assertEquals(productId, producto.getId());
        assertEquals("Yogur", producto.getNombre());
        assertEquals(Set.of(clasificacion), producto.getClasificacionesSalud());
        assertEquals(clasificacionId, clasificacion.getId());
        assertEquals("Sin gluten", clasificacion.getDescripcion());
    }

    @Test
    void clasificacionSaludDtoMapsFromDomain() {
        UUID id = UUID.randomUUID();
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(id);
        clasificacion.setDescripcion("Saludable");

        ClasificacionSaludDTO dto = ClasificacionSaludDTO.fromDomain(clasificacion);

        assertEquals(id, dto.getId());
        assertEquals("Saludable", dto.getDescripcion());
    }

    @Test
    void syncUserRequestPrefersSpanishNamesAndFallsBackToEnglishNames() {
        CognitoUserProfile spanishProfile = new SyncUserRequest(
                "ana@test.com",
                " Ana ",
                " Lopez ",
                "Ann",
                "Lo"
        ).toProfile();
        CognitoUserProfile englishProfile = new SyncUserRequest(
                "bob@test.com",
                " ",
                null,
                " Bob ",
                " Brown "
        ).toProfile();
        CognitoUserProfile emptyProfile = new SyncUserRequest(
                "empty@test.com",
                " ",
                "",
                " ",
                null
        ).toProfile();

        assertEquals("Ana", spanishProfile.nombre());
        assertEquals("Lopez", spanishProfile.apellido());
        assertEquals("Bob", englishProfile.nombre());
        assertEquals("Brown", englishProfile.apellido());
        assertNull(emptyProfile.nombre());
        assertNull(emptyProfile.apellido());
    }

    @Test
    void walletMovementResponseMapsDomainMovement() {
        UUID movementId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 15, 12, 0);
        WalletMovement movement = new WalletMovement(
                movementId,
                dateTime,
                TipoMovimientoBilletera.COMPRA,
                "Compra",
                new BigDecimal("20.00"),
                WalletMovementDirection.SALIDA
        );

        WalletMovementResponse response = WalletMovementResponse.fromDomain(movement);

        assertEquals(movementId, response.id());
        assertEquals(dateTime, response.fechaHora());
        assertEquals("COMPRA", response.tipo());
        assertEquals(WalletMovementDirection.SALIDA, response.direccion());
    }

    @Test
    void menuProductoDtoStaticFactoriesExposeManualFields() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        MenuProductoDTO.CategoryDTO category =
                new MenuProductoDTO.CategoryDTO(categoryId, "Snacks");

        MenuProductoDTO allowed = MenuProductoDTO.permitido(
                productId,
                "Jugo",
                "Natural",
                new BigDecimal("10.00"),
                category
        );
        MenuProductoDTO blocked = MenuProductoDTO.bloqueado(
                productId,
                "Jugo",
                "Natural",
                new BigDecimal("10.00"),
                "Sin disponibilidad",
                category
        );

        assertEquals(productId, allowed.id());
        assertEquals(false, allowed.bloqueado());
        assertNull(allowed.motivoBloqueo());
        assertEquals(categoryId, blocked.categoria().id());
        assertEquals(true, blocked.bloqueado());
        assertEquals("Sin disponibilidad", blocked.motivoBloqueo());
    }

    @Test
    void menuProductoDtoFromProductShouldMapNullAndPresentInventoryFields() {
        Producto withoutCategory = new Producto();
        withoutCategory.setId(UUID.randomUUID());
        withoutCategory.setNombre("Agua");
        withoutCategory.setPrecio(BigDecimal.TEN);

        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Bebidas");
        Producto withInventory = new Producto();
        withInventory.setId(UUID.randomUUID());
        withInventory.setNombre("Jugo");
        withInventory.setPrecio(BigDecimal.TEN);
        withInventory.setCategoria(category);
        withInventory.setEstadoInventario(EstadoInventario.DISPONIBLE);
        withInventory.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        MenuProductoDTO nullCategory = MenuProductoDTO.permitido(withoutCategory);
        MenuProductoDTO mappedInventory = MenuProductoDTO.permitido(withInventory);

        assertNull(nullCategory.categoria());
        assertEquals("DISPONIBLE", mappedInventory.estadoInventario());
        assertEquals("STOCK_EXACTO", mappedInventory.tipoManejoInventario());
        assertEquals(category.getId(), mappedInventory.categoria().id());
    }

    @Test
    void productResponseDtoShouldMapCategoryClassificationsAndNullCollections() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Bebidas");
        ClasificacionSalud health = new ClasificacionSalud();
        health.setId(UUID.randomUUID());
        health.setDescripcion("Sin TACC");
        ClasificacionTipo type = new ClasificacionTipo();
        type.setId(UUID.randomUUID());
        type.setDescripcion("Frio");
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Jugo");
        product.setCategoria(category);
        product.setClasificacionesSalud(Set.of(health));
        product.setTipos(Set.of(type));

        ProductResponseDTO response = ProductResponseDTO.fromDomain(product);

        assertEquals(category.getId(), response.categoria().id());
        assertEquals(1, response.clasificacionesSalud().size());
        assertEquals(1, response.tipos().size());

        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        product.setTipos(null);
        ProductResponseDTO nullResponse = ProductResponseDTO.fromDomain(product);

        assertNull(nullResponse.categoria());
        assertEquals(Set.of(), nullResponse.clasificacionesSalud());
        assertEquals(Set.of(), nullResponse.tipos());
    }

    @Test
    void simpleResponseDtosShouldMapDomainRecordsAndMutableWeather() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Snacks");
        category.setActivo(true);
        WalletHealthSpending healthSpending = new WalletHealthSpending(
                "Saludable",
                new BigDecimal("30.00"),
                new BigDecimal("50.00")
        );
        WalletCategorySpending categorySpending = new WalletCategorySpending(
                "Snacks",
                new BigDecimal("20.00"),
                new BigDecimal("25.00")
        );
        WeatherDTO weather = new WeatherDTO(20.0, "Soleado");

        weather.setTemperature(21.0);
        weather.setCondition("Nublado");

        assertEquals(category.getId(), CategoryResponse.fromDomain(category).id());
        assertEquals("Saludable",
                WalletHealthSpendingResponse.fromDomain(healthSpending).clasificacion());
        assertEquals("Snacks",
                WalletCategorySpendingResponse.fromDomain(categorySpending).categoria());
        assertEquals(21.0, weather.getTemperature());
        assertEquals("Nublado", weather.getCondition());
    }

    @Test
    void scheduledPickupResponseMapsFullPurchaseAndNullBranches() {
        Purchase purchase = purchase();

        ScheduledPickupResponseDTO response = new ScheduledPickupResponseDTO(purchase);
        ScheduledPickupResponseDTO emptyResponse = new ScheduledPickupResponseDTO(new Purchase());

        assertEquals(purchase.getId(), response.getId());
        assertEquals("Ana Lopez", response.getStudentName());
        assertEquals("PENDIENTE", response.getStatus());
        assertEquals(EstadoCompra.PENDIENTE.getLabel(), response.getStatusLabel());
        assertEquals("PROGRAMADO", response.getWithdrawalStatus());
        assertEquals("CREDITOS", response.getPaymentMethod());
        assertEquals(1, response.getItems().size());
        assertNull(emptyResponse.getStudentId());
        assertEquals(List.of(), emptyResponse.getItems());
    }

    @Test
    void purchaseResponseDtoShouldFormatPickupStartTime() {
        Purchase purchase = purchase();
        purchase.setPickupSlotStartTime(LocalTime.of(9, 30));

        PurchaseResponseDTO response = new PurchaseResponseDTO(purchase);

        assertEquals("09:30", response.getPickupSlotStartTime());
    }

    @Test
    void kiosqueroOrderResponseMapsNullsAndInventoryProductFallbacks() {
        Purchase purchase = purchase();
        Producto replacement = new Producto();
        replacement.setId(purchase.getItems().keySet().iterator().next().getId());
        replacement.setNombre("Producto actualizado");
        replacement.setPrecio(new BigDecimal("30.00"));
        replacement.setRequierePreparacion(true);
        replacement.setCupoMaximoDiario(10);

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(
                purchase,
                Map.of(replacement.getId(), replacement)
        );
        KiosqueroOrderResponse nullResponse = KiosqueroOrderResponse.fromDomain(new Purchase());

        assertEquals(purchase.getId(), response.id());
        assertEquals("Ana", response.student().firstName());
        assertEquals("1", response.student().gradeYear());
        assertEquals("TUTOR", response.buyer().type());
        assertEquals("Producto actualizado", response.items().get(0).productName());
        assertEquals(new BigDecimal("30.00"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("30.00"), response.items().get(0).subtotal());
        assertNull(nullResponse.pickupSlot());
        assertNull(nullResponse.student());
        assertEquals(List.of(), nullResponse.items());
    }

    @Test
    void kiosqueroOrderResponseMapsMissingStudentUserNullItemsAndNullProductPrice() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Agua");
        product.setPrecio(null);
        Purchase purchase = new Purchase();
        purchase.setStudent(student);
        purchase.setItems(Map.of(product, 2));

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(purchase, null);

        assertEquals(student.getId(), response.student().id());
        assertNull(response.student().firstName());
        assertNull(response.student().lastName());
        assertNull(response.student().gradeYear());
        assertNull(response.student().gradeDivision());
        assertEquals(BigDecimal.ZERO, response.items().get(0).unitPrice());
        assertEquals(BigDecimal.ZERO, response.items().get(0).subtotal());

        purchase.setItems(null);
        assertEquals(List.of(), KiosqueroOrderResponse.fromDomain(purchase).items());
    }

    private Purchase purchase() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Lopez");

        Grado grado = new Grado();
        grado.setAnio("1");
        grado.setDivision("A");

        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(usuario);
        student.setNombre("Ana");
        student.setApellido("Lopez");
        student.setGrado(grado);

        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Yogur");
        product.setPrecio(new BigDecimal("20.00"));

        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setStudent(student);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("20.00"));
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 6, 15, 10, 0));
        purchase.setWithdrawalCode("1234");
        purchase.setWithdrawalStatus(EstadoRetiro.PROGRAMADO);
        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupSlotStart(LocalTime.of(9, 30));
        purchase.setPickupSlotEnd(LocalTime.of(9, 45));
        purchase.setPickupDate(LocalDate.of(2026, 6, 15));
        purchase.setBuyerId(UUID.randomUUID());
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }
}
