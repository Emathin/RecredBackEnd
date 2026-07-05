package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetActiveLevels;
import com.recred.backend.application.GetActiveLevelsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/niveles")
public class LevelController {

    private final GetActiveLevels getActiveLevels;

    public LevelController(GetActiveLevels getActiveLevels) {
        this.getActiveLevels = getActiveLevels;
    }

    @GetMapping
    public ResponseEntity<List<LevelResponseDTO>> getLevels() {
        List<LevelResponseDTO> levels = getActiveLevels
                .execute(new GetActiveLevelsRequest())
                .levels()
                .stream()
                .map(LevelResponseDTO::fromApplication)
                .toList();

        return ResponseEntity.ok(levels);
    }
}
