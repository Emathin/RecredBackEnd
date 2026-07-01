package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @JsonProperty("nombre")
        @JsonAlias("name")
        @NotBlank(message = "Supplier name is required")
        @Size(max = 200, message = "Name cannot exceed 200 characters")
        String name,

        @JsonProperty("telefono")
        @JsonAlias("phone")
        @Size(max = 100, message = "Phone cannot exceed 100 characters")
        String phone,

        @JsonProperty("email")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        String email,

        @JsonProperty("diasVisita")
        @JsonAlias("visitDays")
        @Size(max = 200, message = "Visit days cannot exceed 200 characters")
        String visitDays,

        @JsonProperty("notas")
        @JsonAlias("notes")
        String notes
) {}
