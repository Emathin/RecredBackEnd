package com.recred.backend.application;

import com.recred.backend.dominio.ClimaService;
import com.recred.backend.dominio.GeminiService;
import com.recred.backend.infrastructure.web.ClimaDTO;
import com.recred.backend.infrastructure.web.RecomendacionResponseDTO;

import org.springframework.stereotype.Service;

@Service
public class GenerarSugerenciaPorEstacion {
    private final ClimaService climaService;
    private final GeminiService geminiService;

    public GenerarSugerenciaPorEstacion(ClimaService climaService, GeminiService geminiService) {
        this.climaService = climaService;
        this.geminiService = geminiService;
    }

    public RecomendacionResponseDTO ejecutarLogicaSugerencias() {
        // Coordenadas de ejemplo por ahora...
        double latitud = -34.70;
        double longitud = -58.47;

        // 1. Obtener el contexto del clima
        ClimaDTO climaActual = climaService.obtenerClimaActual(latitud, longitud);

        System.out.println("Temperatura en el kiosco: " + climaActual.getTemperatura() + "°C");
        System.out.println("Condición climática: " + climaActual.getCondicion());

        // armar el prompt con climaActual.getTemperatura() y climaActual.getCondicion()
        // llamar a geminiService.obtenerSugerencias(prompt)
        String mesActual = "Mayo";
        String etapaEscolar = "Pleno ciclo lectivo";
        String nivelEscolar = "Primaria";

        // 3. Delegar a Gemini la generación de sugerencias estructuradas
        RecomendacionResponseDTO sugerenciasFinales = geminiService.obtenerSugerenciasIas(
                mesActual,
                etapaEscolar,
                nivelEscolar,
                climaActual.getTemperatura(),
                climaActual.getCondicion());

        // 4. Retornar los datos listos para el controlador
        return sugerenciasFinales;
    }
}
