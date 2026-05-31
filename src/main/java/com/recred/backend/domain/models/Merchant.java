package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Merchant {

    private UUID id;
    private User user;
    private String cuit;
    private Buffet buffet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}