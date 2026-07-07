package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.PaymentPurpose;
import com.recred.backend.domain.models.PaymentReference;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReferenceTest {

    @Test
    void toExternalReference_WhenReferenceIsTopUp_ShouldBuildTypedReference() {
        UUID studentId = UUID.randomUUID();
        PaymentReference reference = givenTopUpReference(studentId);

        String result = whenConvertingToExternalReference(reference);

        assertEquals("TOP_UP|" + studentId, result);
    }

    @Test
    void fromExternalReference_WhenReferenceIsLegacyUuid_ShouldParseTopUp() {
        UUID studentId = UUID.randomUUID();

        PaymentReference result = whenParsingReference(studentId.toString());

        assertEquals(PaymentPurpose.TOP_UP, result.purpose());
        assertEquals(studentId, result.targetId());
        assertTrue(result.isTopUp());
    }

    @Test
    void fromExternalReference_WhenReferenceIsUserSubscription_ShouldParseSubscription() {
        UUID userId = UUID.randomUUID();
        String externalReference = givenSubscriptionExternalReference(userId);

        PaymentReference result = whenParsingReference(externalReference);

        assertEquals(PaymentPurpose.USER_SUBSCRIPTION, result.purpose());
        assertEquals(userId, result.targetId());
        assertEquals(TipoPlan.AVANZADO, result.plan());
        assertEquals(PeriodoSuscripcion.ANUAL, result.periodo());
        assertTrue(result.isUserSubscription());
    }

    @Test
    void fromExternalReference_WhenReferenceIsSchoolSubscription_ShouldParseSubscription() {
        UUID schoolId = UUID.randomUUID();
        String externalReference = givenSchoolSubscriptionExternalReference(schoolId);

        PaymentReference result = whenParsingReference(externalReference);

        assertEquals(PaymentPurpose.SCHOOL_SUBSCRIPTION, result.purpose());
        assertEquals(schoolId, result.targetId());
        assertTrue(result.isSchoolSubscription());
    }

    private PaymentReference givenTopUpReference(UUID studentId) {
        return PaymentReference.topUp(studentId);
    }

    private String givenSubscriptionExternalReference(UUID userId) {
        return PaymentReference.userSubscription(
                userId,
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL
        ).toExternalReference();
    }

    private String givenSchoolSubscriptionExternalReference(UUID schoolId) {
        return PaymentReference.schoolSubscription(schoolId).toExternalReference();
    }

    private String whenConvertingToExternalReference(PaymentReference reference) {
        return reference.toExternalReference();
    }

    private PaymentReference whenParsingReference(String externalReference) {
        return PaymentReference.fromExternalReference(externalReference);
    }
}
