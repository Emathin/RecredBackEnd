package com.recred.backend.domain.services;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import net.datafaker.Faker;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class WithdrawalCodeServiceImpl implements WithdrawalCodeService {

    private final PurchaseRepository purchaseRepository;
    private final Faker faker;

    public WithdrawalCodeServiceImpl(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
        this.faker = new Faker(new Locale("es", "AR"));
    }

    @Override
    public String generateCode(Purchase purchase) {
        String code;
        boolean codigoEnUso;
        int intentos = 0;

        do {
            String name = cleanWord(faker.name().firstName());
            String instrument = cleanWord(faker.music().instrument());

            code = name + "-" + instrument;

            codigoEnUso = purchaseRepository.existsByVerificationCodeAndStatusIn(
                    code,
                    List.of(EstadoCompra.PENDIENTE,
                            EstadoCompra.EN_PREPARACION, EstadoCompra.LISTO));

            intentos++;
            if (intentos > 10) {
                throw new RuntimeException(
                        "No se pudo generar un código único de retiro temporal.");
            }

        } while (codigoEnUso);

        return code;
    }

    private String cleanWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return "";
        }
        // Take the first word (in case of names like "Juan Carlos"
        // or instruments like "Guitarra Eléctrica")
        String firstWord = word.trim().split("\\s+")[0];
        // Remove accents / diacritics
        String normalized = Normalizer.normalize(firstWord, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[^\\p{ASCII}]", "");
        // Remove any non-alphanumeric characters
        normalized = normalized.replaceAll("[^a-zA-Z0-9]", "");
        return normalized.toUpperCase();
    }
}