package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Buffet {

    private UUID id;

    private School school;
    private Merchant merchant;

    private String name;
    private LocalDate contractExpirationDate;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}