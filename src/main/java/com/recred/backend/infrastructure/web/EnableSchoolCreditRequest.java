package com.recred.backend.infrastructure.web;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EnableSchoolCreditRequest {
    private UUID parentId;
    private UUID studentId;
}
