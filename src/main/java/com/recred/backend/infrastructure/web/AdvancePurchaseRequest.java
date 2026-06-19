package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record AdvancePurchaseRequest(@Valid @NotEmpty List<OrderRequest> orders) {
    public record OrderRequest(
            @NotNull
            UUID studentId,
            UUID buyerId,
            BuyerType buyerType,
            @NotNull
            UUID buffetId,
            @NotNull
            String date,
            @NotNull
            RecessTime recessTime,
            @Valid @NotEmpty
            List<ItemRequest> items,
            UUID suggestionId,
            UUID tutorId
    ) {
    }

    public record ItemRequest(
            @NotNull
            UUID productId,
            @Positive
            int quantity
    ) {
    }
}
