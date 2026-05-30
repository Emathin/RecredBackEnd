package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.SchoolAssistantCommand;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia/alumnos/{alumnoId}/asistente")
public class SchoolAssistantController {

    private final AskSchoolAssistant askSchoolAssistant;

    public SchoolAssistantController(AskSchoolAssistant askSchoolAssistant) {
        this.askSchoolAssistant = askSchoolAssistant;
    }

    @PostMapping("/mensajes")
    public SchoolAssistantResponse ask(
            @PathVariable("alumnoId") UUID studentId,
            @RequestBody SchoolAssistantRequest request
    ) {
        SchoolAssistantCommand command = new SchoolAssistantCommand(
                request.optionalSessionId(),
                request.message()
        );
        return SchoolAssistantResponse.fromDomain(askSchoolAssistant.execute(
                studentId,
                command
        ));
    }
}
