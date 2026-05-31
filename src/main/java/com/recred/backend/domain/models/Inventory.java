package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private UUID id;
    private UUID buffetId;
    private UUID productId;
    private Integer currentStock;
    private Integer minimumStock;
    private InventoryState stock;
    private LocalDateTime updatedAt;
}
