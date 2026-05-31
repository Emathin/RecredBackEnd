package com.recred.backend.domain.repositories;
import com.recred.backend.domain.models.Merchant;

import java.util.List;
import java.util.UUID;

public interface MerchantRepository {
    UUID findBuffetById(UUID kiosqueroId);

    List<Merchant> findAll();
}
