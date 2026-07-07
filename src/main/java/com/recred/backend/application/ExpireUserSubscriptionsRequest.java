package com.recred.backend.application;

import java.time.LocalDateTime;

public record ExpireUserSubscriptionsRequest(LocalDateTime targetTime) {
}
