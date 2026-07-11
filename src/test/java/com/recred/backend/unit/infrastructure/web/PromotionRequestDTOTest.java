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
        PromotionRequestDTO request = givenValidRequest();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenHasNoViolations(violations);
    }

    @Test
    void missingNameIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithNullName();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "Promotion name is required");
    }

    @Test
    void blankNameIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithBlankName();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "Promotion name is required");
    }

    @Test
    void missingDiscountIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithNullDiscount();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "Discount percentage is required");
    }

    @Test
    void missingStartDateIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithNullStartDate();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "Start date is required");
    }

    @Test
    void missingEndDateIsRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithNullEndDate();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "End date is required");
    }

    @Test
    void missingProductsAreRejectedAtWebBoundary() {
        PromotionRequestDTO request = givenRequestWithNullProducts();

        Set<ConstraintViolation<PromotionRequestDTO>> violations = whenValidating(request);

        thenViolationMessageIs(violations, "Products are required");
    }

    private PromotionRequestDTO givenValidRequest() {
        PromotionRequestDTO request = new PromotionRequestDTO();
        request.setName("Combo");
        request.setDiscountPercentage(new BigDecimal("15.00"));
        request.setStartDate(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 0));
        request.setProductIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        return request;
    }

    private PromotionRequestDTO givenRequestWithNullName() {
        PromotionRequestDTO request = givenValidRequest();
        request.setName(null);
        return request;
    }

    private PromotionRequestDTO givenRequestWithBlankName() {
        PromotionRequestDTO request = givenValidRequest();
        request.setName("   ");
        return request;
    }

    private PromotionRequestDTO givenRequestWithNullDiscount() {
        PromotionRequestDTO request = givenValidRequest();
        request.setDiscountPercentage(null);
        return request;
    }

    private PromotionRequestDTO givenRequestWithNullStartDate() {
        PromotionRequestDTO request = givenValidRequest();
        request.setStartDate(null);
        return request;
    }

    private PromotionRequestDTO givenRequestWithNullEndDate() {
        PromotionRequestDTO request = givenValidRequest();
        request.setEndDate(null);
        return request;
    }

    private PromotionRequestDTO givenRequestWithNullProducts() {
        PromotionRequestDTO request = givenValidRequest();
        request.setProductIds(null);
        return request;
    }

    private Set<ConstraintViolation<PromotionRequestDTO>> whenValidating(PromotionRequestDTO request) {
        return validator.validate(request);
    }

    private void thenHasNoViolations(Set<ConstraintViolation<PromotionRequestDTO>> violations) {
        assertTrue(violations.isEmpty());
    }

    private void thenViolationMessageIs(
            Set<ConstraintViolation<PromotionRequestDTO>> violations,
            String expectedMessage
    ) {
        assertEquals(1, violations.size());
        assertEquals(expectedMessage, violations.iterator().next().getMessage());
    }
}
