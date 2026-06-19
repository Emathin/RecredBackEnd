package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.models.Alumno;

import java.math.BigDecimal;
import java.util.UUID;

public class GetLastRecharge {

    private final AlumnoRepository alumnoRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public GetLastRecharge(
            AlumnoRepository alumnoRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.alumnoRepository = alumnoRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    public BigDecimal execute(UUID studentId) {
        Alumno alumno = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Billetera wallet = alumno.getBilletera();
        if (wallet == null) {
            return BigDecimal.ZERO;
        }

        return movimientoBilleteraRepository.obtenerUltimoMontoCarga(wallet.getId());
    }
}
