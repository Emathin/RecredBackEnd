package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.PromotionRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validPromotionRequestHasNoStructuralErrors() {
        Set<ConstraintViolation<PromotionRequestDTO>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void missingNameIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setName(null);

        assertViolationMessage(
                validator.validate(request),
                "Promotion name is required");
    }

    @Test
    void blankNameIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setName("   ");

        assertViolationMessage(
                validator.validate(request),
                "Promotion name is required");
    }

    @Test
    void missingDiscountIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setDiscountPercentage(null);

        assertViolationMessage(
                validator.validate(request),
                "Discount percentage is required");
    }

    @Test
    void missingStartDateIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setStartDate(null);

        assertViolationMessage(
                validator.validate(request),
                "Start date is required");
    }

    @Test
    void missingEndDateIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setEndDate(null);

        assertViolationMessage(
                validator.validate(request),
                "End date is required");
    }

    @Test
    void missingProductsAreRejectedAtWebBoundary() {
        PromotionRequestDTO request = validRequest();
        request.setProductIds(null);

        assertViolationMessage(
                validator.validate(request),
                "Products are required");
    }

    private PromotionRequestDTO validRequest() {
        PromotionRequestDTO request = new PromotionRequestDTO();
        request.setName("Combo");
        request.setDiscountPercentage(new BigDecimal("15.00"));
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 0));
        request.setProductIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        return request;
    }

    private void assertViolationMessage(
            Set<ConstraintViolation<PromotionRequestDTO>> violations,
            String expectedMessage) {
        assertEquals(1, violations.size());
        assertEquals(expectedMessage, violations.iterator().next().getMessage());
    }
}
