package com.recred.backend.application;

public record WeatherResponse(
        Double temperature,
        String condition,
        String city,
        String province
) {
}
