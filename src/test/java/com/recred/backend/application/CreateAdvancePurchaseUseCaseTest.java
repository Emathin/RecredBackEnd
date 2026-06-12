package com.recred.backend.application;

import com.recred.backend.domain.exceptions.DietaryRestrictionException;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateAdvancePurchaseUseCaseTest {

        @Mock
        private PurchaseValidator purchaseValidator;
        @Mock
        private AlumnoRepository alumnoRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private PurchaseRepository purchaseRepository;
        @Mock
        private WithdrawalCodeService withdrawalCodeService;
        @Mock
        private WalletRepository walletRepository;
        @Mock
        private FranjaHorariaRepository franjaHorariaRepository;
        @Mock
        private SpringDataBuffetRepository springDataBuffetRepository;
        @Mock
        private UsuarioRepository usuarioRepository;
        @Mock
        private TutorRepository tutorRepository;

        @InjectMocks
        private CreateAdvancePurchase createAdvancePurchase;

        private AutoCloseable closeable;

        @BeforeEach
        void setUp() {
                closeable = MockitoAnnotations.openMocks(this);
                createAdvancePurchase = new CreateAdvancePurchase(
                                purchaseValidator,
                                alumnoRepository,
                                productRepository,
                                purchaseRepository,
                                withdrawalCodeService,
                                walletRepository,
                                franjaHorariaRepository,
                                springDataBuffetRepository,
                                usuarioRepository,
                                tutorRepository);
        }

        @AfterEach
        void tearDown() throws Exception {
                closeable.close();
        }

        @Test
        void execute_shouldReturnPendingStatusAndWithdrawalCodes_whenPurchaseIsSuccessful() {

                UUID studentId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(productId, 1);
                java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
                if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                    tomorrow = tomorrow.plusDays(2);
                } else if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    tomorrow = tomorrow.plusDays(1);
                }
                String testDate = tomorrow.toString();

                AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                                studentId, UUID.randomUUID(), BuyerType.TUTOR, testDate,
                                RecessTime.FIRST_RECESS, List.of(itemRequest));
                AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

                Alumno student = new Alumno();
                student.setId(studentId);

                Grado grado = new Grado();
                Colegio colegio = new Colegio();
                colegio.setId(UUID.randomUUID());
                grado.setColegio(colegio);
                student.setGrado(grado);

                Producto product = new Producto();
                product.setId(productId);
                product.setPrecio(BigDecimal.TEN);
                product.setRequierePreparacion(true);

                Map<Producto, Integer> items = new HashMap<>();
                items.put(product, 1);

                Purchase purchase = new Purchase(student, items, null, studentId, BuyerType.STUDENT);
                purchase.setId(UUID.randomUUID());
                purchase.setWithdrawalCode("test-code");

                BuffetEntity buffetEntity = new BuffetEntity();
                buffetEntity.setId(UUID.randomUUID());

                FranjaHoraria franja = new FranjaHoraria();
                franja.setId(UUID.randomUUID());
                franja.setDescripcion("Primer Recreo");
                franja.setHoraInicio(java.time.LocalTime.of(9, 30));

                when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(springDataBuffetRepository.findFirstByColegioEntityIdAndActivoTrue(any(UUID.class)))
                                .thenReturn(Optional.of(buffetEntity));
                when(franjaHorariaRepository.findAllByColegioId(any(UUID.class)))
                                .thenReturn(List.of(franja));
                when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
                when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("test-code");

                Usuario mockUsuario = new Usuario();
                mockUsuario.setId(UUID.randomUUID());
                mockUsuario.setRol(RolUsuario.PADRE);
                Tutor mockTutor = new Tutor();
                mockTutor.setHijos(Set.of(student));
                mockUsuario.setTutor(mockTutor);
                when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(mockUsuario));
                when(tutorRepository.findByUsuarioId(mockUsuario.getId())).thenReturn(Optional.of(mockTutor));

                Billetera mockWallet = new Billetera();
                mockWallet.setId(UUID.randomUUID());

                mockWallet.setAlumno(student);
                mockWallet.setSaldoCreditos(new BigDecimal("100.0"));
                when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(mockWallet));

                AdvancePurchaseResponse response = createAdvancePurchase.execute(request, "test-user");
                assertNotNull(response);
                assertEquals(EstadoCompra.PENDIENTE, response.status().status());
                assertEquals(10.0, response.total());
                assertTrue(response.codes().containsKey(studentId));
                assertEquals("test-code", response.codes().get(studentId));
        }

        @Test
        void execute_shouldThrowDietaryRestrictionException_whenValidationFails() {
                UUID studentId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(productId, 1);
                java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
                if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                    tomorrow = tomorrow.plusDays(2);
                } else if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    tomorrow = tomorrow.plusDays(1);
                }
                String testDate = tomorrow.toString();

                AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                                studentId, UUID.randomUUID(), BuyerType.TUTOR, testDate,
                                RecessTime.FIRST_RECESS, List.of(itemRequest));
                AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

                Alumno student = new Alumno();
                student.setId(studentId);

                Grado grado = new Grado();
                Colegio colegio = new Colegio();
                colegio.setId(UUID.randomUUID());
                grado.setColegio(colegio);
                student.setGrado(grado);

                Producto product = new Producto();
                product.setId(productId);

                BuffetEntity buffetEntity = new BuffetEntity();
                buffetEntity.setId(UUID.randomUUID());

                FranjaHoraria franja = new FranjaHoraria();
                franja.setId(UUID.randomUUID());
                franja.setDescripcion("Primer Recreo");
                franja.setHoraInicio(java.time.LocalTime.of(9, 30));

                when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(springDataBuffetRepository.findFirstByColegioEntityIdAndActivoTrue(any(UUID.class)))
                                .thenReturn(Optional.of(buffetEntity));
                when(franjaHorariaRepository.findAllByColegioId(any(UUID.class)))
                                .thenReturn(List.of(franja));

                Usuario mockUsuario = new Usuario();
                mockUsuario.setId(UUID.randomUUID());
                mockUsuario.setRol(RolUsuario.PADRE);
                Tutor mockTutor = new Tutor();
                mockTutor.setHijos(Set.of(student));
                mockUsuario.setTutor(mockTutor);
                when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(mockUsuario));
                when(tutorRepository.findByUsuarioId(mockUsuario.getId())).thenReturn(Optional.of(mockTutor));

                doThrow(new DietaryRestrictionException("Dietary restriction violated"))
                                .when(purchaseValidator).validate(any(Purchase.class));
                assertThrows(DietaryRestrictionException.class,
                                () -> createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowIllegalArgumentException_whenOrdersListIsEmpty() {
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of());
            assertThrows(IllegalArgumentException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowSecurityException_whenUserNotFound() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, UUID.randomUUID(), BuyerType.STUDENT, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.empty());

            assertThrows(SecurityException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowSecurityException_whenStudentTriesToBuyForAnotherStudent() {
            UUID studentId = UUID.randomUUID();
            UUID anotherStudentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    anotherStudentId, UUID.randomUUID(), BuyerType.STUDENT, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno studentObj = new Alumno();
            studentObj.setId(studentId);
            studentUser.setAlumno(studentObj);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));

            assertThrows(SecurityException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowSecurityException_whenTutorTriesToBuyForNonChild() {
            UUID studentId = UUID.randomUUID();
            UUID nonChildId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    nonChildId, UUID.randomUUID(), BuyerType.TUTOR, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario parentUser = new Usuario();
            parentUser.setId(UUID.randomUUID());
            parentUser.setRol(RolUsuario.PADRE);

            Tutor tutor = new Tutor();
            Alumno childObj = new Alumno();
            childObj.setId(studentId);
            tutor.setHijos(Set.of(childObj));

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(parentUser));
            when(tutorRepository.findByUsuarioId(parentUser.getId())).thenReturn(Optional.of(tutor));

            assertThrows(SecurityException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowSecurityException_whenUserRoleNotAuthorized() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, UUID.randomUUID(), BuyerType.TUTOR, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario adminUser = new Usuario();
            adminUser.setId(UUID.randomUUID());
            adminUser.setRol(RolUsuario.ADMIN);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(adminUser));

            assertThrows(SecurityException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowAlumnoNoEncontradoException_whenStudentNotFound() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno studentObj = new Alumno();
            studentObj.setId(studentId);
            studentUser.setAlumno(studentObj);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

            assertThrows(AlumnoNoEncontradoException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowPickupTimeSlotNotFoundException_whenStudentGradeOrSchoolIsNull() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno student = new Alumno();
            student.setId(studentId);
            student.setGrado(null);
            studentUser.setAlumno(student);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

            assertThrows(PickupTimeSlotNotFoundException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowRuntimeException_whenBuffetNotFound() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, "2026-10-10",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno student = new Alumno();
            student.setId(studentId);
            Grado grado = new Grado();
            Colegio colegio = new Colegio();
            colegio.setId(UUID.randomUUID());
            grado.setColegio(colegio);
            student.setGrado(grado);
            studentUser.setAlumno(student);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(springDataBuffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(colegio.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowIllegalArgumentException_whenPickupDateIsWeekend() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, "2026-06-14",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno student = new Alumno();
            student.setId(studentId);
            Grado grado = new Grado();
            Colegio colegio = new Colegio();
            colegio.setId(UUID.randomUUID());
            grado.setColegio(colegio);
            student.setGrado(grado);
            studentUser.setAlumno(student);

            BuffetEntity buffet = new BuffetEntity();
            buffet.setId(UUID.randomUUID());

            FranjaHoraria franja = new FranjaHoraria();
            franja.setId(UUID.randomUUID());
            franja.setDescripcion("Primer Recreo");
            franja.setHoraInicio(java.time.LocalTime.of(9, 30));

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(springDataBuffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(colegio.getId()))
                    .thenReturn(Optional.of(buffet));
            when(franjaHorariaRepository.findAllByColegioId(colegio.getId()))
                    .thenReturn(List.of(franja));

            assertThrows(IllegalArgumentException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowIllegalArgumentException_whenPickupDateIsPast() {
            UUID studentId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest item = new AdvancePurchaseRequest.ItemRequest(
                    UUID.randomUUID(), 1);
            AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, "2025-06-11",
                    RecessTime.FIRST_RECESS, List.of(item));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            Alumno student = new Alumno();
            student.setId(studentId);
            Grado grado = new Grado();
            Colegio colegio = new Colegio();
            colegio.setId(UUID.randomUUID());
            grado.setColegio(colegio);
            student.setGrado(grado);
            studentUser.setAlumno(student);

            BuffetEntity buffet = new BuffetEntity();
            buffet.setId(UUID.randomUUID());

            FranjaHoraria franja = new FranjaHoraria();
            franja.setId(UUID.randomUUID());
            franja.setDescripcion("Primer Recreo");
            franja.setHoraInicio(java.time.LocalTime.of(9, 30));

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(springDataBuffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(colegio.getId()))
                    .thenReturn(Optional.of(buffet));
            when(franjaHorariaRepository.findAllByColegioId(colegio.getId()))
                    .thenReturn(List.of(franja));

            assertThrows(IllegalArgumentException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowIllegalStateException_whenWalletNotFound() {
            UUID studentId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(
                    productId, 1);

            java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
            if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                tomorrow = tomorrow.plusDays(2);
            } else if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                tomorrow = tomorrow.plusDays(1);
            }
            String testDate = tomorrow.toString();

            AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, testDate,
                    RecessTime.FIRST_RECESS, List.of(itemRequest));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

            Alumno student = new Alumno();
            student.setId(studentId);
            Grado grado = new Grado();
            Colegio colegio = new Colegio();
            colegio.setId(UUID.randomUUID());
            grado.setColegio(colegio);
            student.setGrado(grado);

            Producto product = new Producto();
            product.setId(productId);
            product.setPrecio(BigDecimal.TEN);

            BuffetEntity buffetEntity = new BuffetEntity();
            buffetEntity.setId(UUID.randomUUID());

            FranjaHoraria franja = new FranjaHoraria();
            franja.setId(UUID.randomUUID());
            franja.setDescripcion("Primer Recreo");
            franja.setHoraInicio(java.time.LocalTime.of(9, 30));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            studentUser.setAlumno(student);

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(springDataBuffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(colegio.getId()))
                    .thenReturn(Optional.of(buffetEntity));
            when(franjaHorariaRepository.findAllByColegioId(colegio.getId()))
                    .thenReturn(List.of(franja));
            when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }

        @Test
        void execute_shouldThrowInsufficientBalanceException_whenWalletBalanceIsInsufficient() {
            UUID studentId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(
                    productId, 1);

            java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
            if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                tomorrow = tomorrow.plusDays(2);
            } else if (tomorrow.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                tomorrow = tomorrow.plusDays(1);
            }
            String testDate = tomorrow.toString();

            AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                    studentId, studentId, BuyerType.STUDENT, testDate,
                    RecessTime.FIRST_RECESS, List.of(itemRequest));
            AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

            Alumno student = new Alumno();
            student.setId(studentId);
            Grado grado = new Grado();
            Colegio colegio = new Colegio();
            colegio.setId(UUID.randomUUID());
            grado.setColegio(colegio);
            student.setGrado(grado);

            Producto product = new Producto();
            product.setId(productId);
            product.setPrecio(BigDecimal.TEN);

            BuffetEntity buffetEntity = new BuffetEntity();
            buffetEntity.setId(UUID.randomUUID());

            FranjaHoraria franja = new FranjaHoraria();
            franja.setId(UUID.randomUUID());
            franja.setDescripcion("Primer Recreo");
            franja.setHoraInicio(java.time.LocalTime.of(9, 30));

            Usuario studentUser = new Usuario();
            studentUser.setId(UUID.randomUUID());
            studentUser.setRol(RolUsuario.ALUMNO);
            studentUser.setAlumno(student);

            Billetera mockWallet = new Billetera();
            mockWallet.setId(UUID.randomUUID());
            mockWallet.setAlumno(student);
            mockWallet.setSaldoCreditos(new BigDecimal("5.0"));

            when(usuarioRepository.findByIdCognito("test-user")).thenReturn(Optional.of(studentUser));
            when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(springDataBuffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(colegio.getId()))
                    .thenReturn(Optional.of(buffetEntity));
            when(franjaHorariaRepository.findAllByColegioId(colegio.getId()))
                    .thenReturn(List.of(franja));
            when(walletRepository.findByStudentId(studentId))
                    .thenReturn(Optional.of(mockWallet));

            assertThrows(InsufficientBalanceException.class, () ->
                    createAdvancePurchase.execute(request, "test-user"));
        }
}
