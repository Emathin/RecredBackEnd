package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateAdvancePurchase {

    private final PurchaseValidator purchaseValidator;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;
    private final UsuarioRepository usuarioRepository;
    private final WalletRepository walletRepository;

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            UsuarioRepository usuarioRepository,
            WalletRepository walletRepository
    ) {
        this.purchaseValidator = purchaseValidator;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
        this.usuarioRepository = usuarioRepository;
        this.walletRepository = walletRepository;
    }

    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request, String cognitoSub) {
        AdvancePurchaseRequest.OrderRequest orderRequest = request.orders().get(0);

        Usuario usuario = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        UUID buyerId = usuario.getId();
        BuyerType buyerType;

        if (usuario.getRol() == RolUsuario.ALUMNO) {
            if (usuario.getAlumno() == null || 
                !usuario.getAlumno().getId().equals(orderRequest.studentId())) {
                throw new SecurityException("No autorizado para comprar a nombre de otro alumno");
            }
            buyerType = BuyerType.STUDENT;
        } else if (usuario.getRol() == RolUsuario.PADRE) {
            if (usuario.getTutor() == null || 
                usuario.getTutor().getHijos().stream()
                        .noneMatch(h -> h.getId().equals(orderRequest.studentId()))) {
                throw new SecurityException("No autorizado para comprar a nombre de este alumno");
            }
            buyerType = BuyerType.TUTOR;
        } else {
            throw new SecurityException("Rol no autorizado para realizar compras");
        }

        Alumno student = alumnoRepository.findById(orderRequest.studentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Map<Producto, Integer> items = new HashMap<>();
        for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
            Producto product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            items.put(product, itemRequest.quantity());
        }

        Purchase purchase = new Purchase(
                student,
                items,
                MedioPago.CREDITOS,
                buyerId,
                buyerType
        );

        purchaseValidator.validate(purchase);

        Billetera billetera = walletRepository.findByStudentId(student.getId())
                .orElseThrow(() -> new RuntimeException("Billetera no encontrada para el alumno"));
        
        billetera.debit(purchase.getTotalAmount());
        walletRepository.save(billetera);

        purchase = purchaseRepository.save(purchase);

        String withdrawalCode = withdrawalCodeService.generateCode(purchase);
        Map<UUID, String> codes = new HashMap<>();
        codes.put(student.getId(), withdrawalCode);

        return new AdvancePurchaseResponse(
                purchase.getId().toString(),
                PurchaseStatus.from(purchase),
                purchase.getTotalAmount().doubleValue(),
                codes
        );
    }
}
