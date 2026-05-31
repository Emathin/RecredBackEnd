package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.exceptions.TimeRestrictionException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public class TimeValidatorService {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Argentina/Buenos_Aires");

    public void validatePurchase(Student student, Set<Product> products,
                                 LocalDateTime purchaseMoment) {

        LocalDateTime localMoment = ZonedDateTime.of(purchaseMoment, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL)
                .toLocalDateTime();

        for (Product product : products) {
            boolean isBlocked = student.getTimeRestrictions().stream()
                    .anyMatch(restriction -> restriction.blocks(product, localMoment));

            if (isBlocked) {
                throwException(product);
            }
        }
    }

    private void throwException(Product product) {
        String mensaje = "Las compras del producto '" + product.getName() +
                "' están bloqueadas en este horario por configuración del tutor.";
        throw new TimeRestrictionException(mensaje);
    }
}