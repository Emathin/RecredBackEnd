package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.SupplierRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierRequestTest {

    private Validator validator;
    private Set<ConstraintViolation<SupplierRequest>> violations;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void givenValidRequestWhenValidatingThenHasNoErrors() {
        whenValidating(givenValidRequest());

        thenNoViolations();
    }

    @Test
    void givenMissingNameWhenValidatingThenIsRejected() {
        whenValidating(givenRequestWithName(null));

        thenViolationMessageIs("Supplier name is required");
    }

    @Test
    void givenBlankNameWhenValidatingThenIsRejected() {
        whenValidating(givenRequestWithName("   "));

        thenViolationMessageIs("Supplier name is required");
    }

    private SupplierRequest givenValidRequest() {
        return givenRequestWithName("Proveedor Test");
    }

    private SupplierRequest givenRequestWithName(String name) {
        return new SupplierRequest(
                name, "11223344", "proveedor@test.com",
                "Lunes", "Entrega temprano");
    }

    private void whenValidating(SupplierRequest request) {
        violations = validator.validate(request);
    }

    private void thenNoViolations() {
        assertTrue(violations.isEmpty());
    }

    private void thenViolationMessageIs(String expected) {
        assertEquals(1, violations.size());
        assertEquals(expected,
                violations.iterator().next().getMessage());
    }
}
