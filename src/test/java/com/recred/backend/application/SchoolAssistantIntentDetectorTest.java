package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolAssistantIntentDetectorTest {

    private final SchoolAssistantIntentDetector detector = new SchoolAssistantIntentDetector();

    @Test
    void recessTimeShouldDetectNoonFromLunchMessage() {
        assertEquals(RecessTime.NOON, detector.recessTime("almuerzo").orElseThrow());
        assertEquals(SchoolAssistantIntent.SELECT_RECESS,
                detector.detectIntent("quiero retirar al mediodia"));
    }
}
