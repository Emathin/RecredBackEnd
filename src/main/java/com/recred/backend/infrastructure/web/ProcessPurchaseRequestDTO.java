package com.recred.backend.infrastructure.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPurchaseRequestDTO {
    private UUID studentId;
    private List<PurchaseItemDTO> items;
}
