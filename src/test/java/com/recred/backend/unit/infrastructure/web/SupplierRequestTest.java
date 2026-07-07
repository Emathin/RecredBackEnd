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

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validSupplierRequestHasNoStructuralErrors() {
        Set<ConstraintViolation<SupplierRequest>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void missingNameIsRejectedAtWebBoundary() {
        Set<ConstraintViolation<SupplierRequest>> violations =
                validator.validate(requestWithName(null));

        assertViolationMessage(violations, "Supplier name is required");
    }

    @Test
    void blankNameIsRejectedAtWebBoundary() {
        Set<ConstraintViolation<SupplierRequest>> violations =
                validator.validate(requestWithName("   "));

        assertViolationMessage(violations, "Supplier name is required");
    }

    private SupplierRequest validRequest() {
        return requestWithName("Proveedor Test");
    }

    private SupplierRequest requestWithName(String name) {
        return new SupplierRequest(
                name,
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
    }

    private void assertViolationMessage(
            Set<ConstraintViolation<SupplierRequest>> violations,
            String expectedMessage) {
        assertEquals(1, violations.size());
        assertEquals(expectedMessage, violations.iterator().next().getMessage());
    }
}
