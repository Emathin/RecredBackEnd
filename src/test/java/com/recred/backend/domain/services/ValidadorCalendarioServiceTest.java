package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ValidadorCalendarioServiceTest {

    @Mock
    private SchoolCalendarRepository schoolCalendarRepository;

    private ValidadorCalendarioService validadorCalendarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validadorCalendarioService = new ValidadorCalendarioService(schoolCalendarRepository);
    }

    @Test
    void validarVenta_NoLanzaExcepcion_EnDiaLaboral() {
        UUID colegioId = UUID.randomUUID();
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        
        LocalDate lunes = LocalDate.of(2026, 6, 1); // Lunes
        
        when(schoolCalendarRepository.findByColegioIdAndFecha(colegioId, lunes))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validadorCalendarioService.validarVenta(buffet, lunes));
    }

    @Test
    void validarVenta_LanzaExcepcion_EnSabadoSiNoEstaHabilitado() {
        UUID colegioId = UUID.randomUUID();
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        buffet.setPermitirVentasFinDeSemana(false);
        
        LocalDate sabado = LocalDate.of(2026, 6, 6); // Sabado
        
        when(schoolCalendarRepository.findByColegioIdAndFecha(colegioId, sabado))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> validadorCalendarioService.validarVenta(buffet, sabado));
    }

    @Test
    void validarVenta_NoLanzaExcepcion_EnSabadoSiEstaHabilitado() {
        UUID colegioId = UUID.randomUUID();
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        buffet.setPermitirVentasFinDeSemana(true);
        
        LocalDate sabado = LocalDate.of(2026, 6, 6); // Sabado
        
        when(schoolCalendarRepository.findByColegioIdAndFecha(colegioId, sabado))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validadorCalendarioService.validarVenta(buffet, sabado));
    }

    @Test
    void validarVenta_LanzaExcepcion_EnFeriadoSiNoEstaHabilitado() {
        UUID colegioId = UUID.randomUUID();
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        buffet.setPermitirVentasFeriados(false);
        
        LocalDate feriado = LocalDate.of(2026, 6, 20);
        SchoolCalendarEvent event = new SchoolCalendarEvent(feriado, TipoDiaEscolar.FERIADO, "Dia de la Bandera");
        
        when(schoolCalendarRepository.findByColegioIdAndFecha(colegioId, feriado))
                .thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> validadorCalendarioService.validarVenta(buffet, feriado));
    }

    @Test
    void validarVenta_NoLanzaExcepcion_EnFeriadoSiEstaHabilitado() {
        UUID colegioId = UUID.randomUUID();
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        buffet.setPermitirVentasFeriados(true);
        
        LocalDate feriado = LocalDate.of(2026, 6, 20);
        SchoolCalendarEvent event = new SchoolCalendarEvent(feriado, TipoDiaEscolar.FERIADO, "Dia de la Bandera");
        
        when(schoolCalendarRepository.findByColegioIdAndFecha(colegioId, feriado))
                .thenReturn(Optional.of(event));

        assertDoesNotThrow(() -> validadorCalendarioService.validarVenta(buffet, feriado));
    }
}
