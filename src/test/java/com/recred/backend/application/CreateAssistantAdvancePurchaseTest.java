package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.domain.services.WithdrawalCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAssistantAdvancePurchaseTest {

    @Mock
    private AlumnoRepository studentRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private FranjaHorariaRepository timeSlotRepository;

    @Mock
    private ValidadorHorarioService timeValidator;

    @Mock
    private ValidacionRestriccionesService healthValidator;

    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @Mock
    private WithdrawalCodeService withdrawalCodeService;

    private CreateAssistantAdvancePurchase useCase;
    private UUID studentId;
    private UUID schoolId;
    private UUID buffetId;
    private UUID slotId;
    private Producto product;
    private Billetera wallet;

    @BeforeEach
    void setUp() {
        useCase = new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                timeSlotRepository,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires"))
        );
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        product = product();
        wallet = wallet();
    }

    @Test
    void executeCreatesPendingAdvancePurchaseWithPickupSlotAndWithdrawalCode() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 2);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(timeSlotRepository.findAllByColegioId(schoolId)).thenReturn(List.of(timeSlot()));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("ABC123");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Purchase result = useCase.execute(
                studentId,
                buffetId,
                cart,
                RecessTime.FIRST_RECESS,
                pickupDate
        );

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(TipoCompra.ANTICIPADA, savedPurchase.getTipo());
        assertEquals(EstadoCompra.PENDIENTE, savedPurchase.getStatus());
        assertEquals(buffetId, savedPurchase.getBuffetId());
        assertEquals(slotId, savedPurchase.getPickupSlotId());
        assertEquals(pickupDate, savedPurchase.getPickupDate());
        assertEquals("ABC123", savedPurchase.getWithdrawalCode());
        assertEquals(new BigDecimal("30.00"), savedPurchase.getTotalAmount());
        assertEquals(new BigDecimal("70.00"), wallet.getSaldoCreditos());
        assertEquals(savedPurchase, result);

        verify(timeValidator).validarCompra(
                eq(student),
                eq(cart.keySet()),
                eq(LocalDateTime.of(2026, 5, 27, 10, 0))
        );
        verify(healthValidator).validarCompra(eq(student), eq(cart.keySet()));
        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(new BigDecimal("30.00")),
                eq(Map.of()),
                eq(LocalDateTime.of(2026, 5, 27, 10, 0))
        );
        verify(walletRepository).save(wallet);
    }

    private Alumno student() {
        Colegio school = new Colegio();
        school.setId(schoolId);
        Grado grade = new Grado();
        grade.setColegio(school);
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setGrado(grade);
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setPrecio(new BigDecimal("15.00"));
        return product;
    }

    private Billetera wallet() {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(new BigDecimal("100.00"));
        return wallet;
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(slotId);
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 20));
        return timeSlot;
    }
}
