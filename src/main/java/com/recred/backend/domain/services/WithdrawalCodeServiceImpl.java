package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;
import java.util.UUID;


public class WithdrawalCodeServiceImpl implements WithdrawalCodeService {

    @Override
    public String generateCode(Purchase purchase) {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public WithdrawalCodeServiceImpl() {
    }
}
