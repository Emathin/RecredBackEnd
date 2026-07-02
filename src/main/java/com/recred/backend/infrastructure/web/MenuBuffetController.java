package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateBuffetMenu;
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

    private final GenerateBuffetMenu generateBuffetMenu;
    private final GetBuffetByStudent getBuffetByStudent;

    public MenuBuffetController(GenerateBuffetMenu generateBuffetMenu,
            GetBuffetByStudent getBuffetByStudent) {
        this.generateBuffetMenu = generateBuffetMenu;
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

        List<MenuProductoDTO> menu = generateBuffetMenu.execute(
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
