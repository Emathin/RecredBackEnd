package com.recred.backend.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPurchaseRequestDTO {
    @NotNull
    private UUID studentId;
    @NotNull
    private UUID buffetId;
    @Valid
    @NotEmpty
    private List<PurchaseItemDTO> items;

}
