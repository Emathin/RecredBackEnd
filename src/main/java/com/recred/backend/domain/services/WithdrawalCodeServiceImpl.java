package com.recred.backend.domain.services;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import net.datafaker.Faker;

import java.util.List;
import java.util.Locale;

public class WithdrawalCodeServiceImpl implements WithdrawalCodeService {

    private final Faker faker = new Faker(new Locale("es", "AR"));
    private final PurchaseRepository purchaseRepository;

    public WithdrawalCodeServiceImpl(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public String generateCode(Purchase purchase) {
        String code;
        boolean codigoEnUso;
        int intentos = 0;

        do {
            String animal = faker.animal().name().toUpperCase().replace(" ", "");
            String color = faker.color().name().toUpperCase().replace(" ", "");

            code = animal + "-" + color;

            codigoEnUso = purchaseRepository.existsByVerificationCodeAndStatusIn(
                    code,
                    List.of(EstadoCompra.PENDIENTE,
                            EstadoCompra.EN_PREPARACION, EstadoCompra.LISTO));

            intentos++;
            if (intentos > 10) {
                throw new RuntimeException
                        ("No se pudo generar un código único de retiro temporal.");
            }

        } while (codigoEnUso);

        return code;
    }
}