package com.recred.backend.application;

import java.util.List;

public record GetTutorHistoryResponse(
        List<GetHistoryByStudentResponse> purchases
) {
}
