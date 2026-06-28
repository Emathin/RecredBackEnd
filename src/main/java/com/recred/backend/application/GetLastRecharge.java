package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.models.Alumno;

import java.math.BigDecimal;


public class GetLastRecharge {

    private final AlumnoRepository alumnoRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public GetLastRecharge(
            AlumnoRepository alumnoRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.alumnoRepository = alumnoRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    public GetLastRechargeResponse execute(GetLastRechargeRequest request) {
        request.validate();
        
        Alumno alumno = alumnoRepository.findById(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Billetera wallet = getWallet(alumno);
        if (wallet == null) {
            return new GetLastRechargeResponse(BigDecimal.ZERO);
        }

        BigDecimal lastAmount = 
                movimientoBilleteraRepository.obtenerUltimoMontoCarga(wallet.getId());
        return new GetLastRechargeResponse(lastAmount);
    }

    private Billetera getWallet(Alumno alumno) {
        return alumno.getBilletera();
    }
}
