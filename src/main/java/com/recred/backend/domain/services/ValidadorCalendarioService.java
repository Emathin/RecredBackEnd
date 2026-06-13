package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class ValidadorCalendarioService {

    private final SchoolCalendarRepository schoolCalendarRepository;

    public ValidadorCalendarioService(SchoolCalendarRepository schoolCalendarRepository) {
        this.schoolCalendarRepository = schoolCalendarRepository;
    }

    public void validarVenta(Buffet buffet, LocalDate fecha) {
        if (buffet == null || fecha == null) {
            return;
        }

        UUID colegioId = buffet.getColegio().getId();
        Optional<SchoolCalendarEvent> evento = schoolCalendarRepository
                .findByColegioIdAndFecha(colegioId, fecha);

        if (evento.isPresent()) {
            TipoDiaEscolar tipoDia = evento.get().type();
            if (tipoDia == TipoDiaEscolar.FERIADO || tipoDia == TipoDiaEscolar.SIN_ACTIVIDAD) {
                if (!Boolean.TRUE.equals(buffet.getPermitirVentasFeriados())) {
                    throw new IllegalArgumentException(
                            "Ventas no permitidas en feriados o días sin actividad");
                }
            }
        }

        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            if (!Boolean.TRUE.equals(buffet.getPermitirVentasFinDeSemana())) {
                throw new IllegalArgumentException("Ventas no permitidas los fines de semana");
            }
        }
    }
}
