package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerarMenuBuffet;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alumnos")
public class MenuBuffetController {

    private final GenerarMenuBuffet generarMenuBuffet;
    private final GetBuffetByStudent getBuffetByStudent;

    public MenuBuffetController(GenerarMenuBuffet generarMenuBuffet,
            GetBuffetByStudent getBuffetByStudent) {
        this.generarMenuBuffet = generarMenuBuffet;
        this.getBuffetByStudent = getBuffetByStudent;
    }

    @GetMapping("/{alumnoId}/menu-buffet")
    public ResponseEntity<List<MenuProductoDTO>> obtenerMenu(
            @PathVariable UUID alumnoId,
            @RequestParam UUID buffetId,
            @RequestParam(required = false) LocalDateTime fechaHoraConsulta) {

        LocalDateTime momentoEvaluacion = (fechaHoraConsulta != null)
                ? fechaHoraConsulta
                : LocalDateTime.now();

        List<MenuProductoDTO> menu = generarMenuBuffet.execute(
                alumnoId,
                buffetId,
                momentoEvaluacion
        );

        return ResponseEntity.ok(menu);
    }

    @GetMapping("/{alumnoId}/buffet")
    public ResponseEntity<AlumnoBuffetResponseDTO> obtenerBuffet(
            @PathVariable UUID alumnoId) {
        return getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))
                .map(AlumnoBuffetResponseDTO::fromResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
