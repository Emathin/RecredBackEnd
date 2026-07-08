package com.recred.backend.application;

import java.util.UUID;

public record SchoolAssistantConversationOwner(
        UUID id,
        SchoolAssistantConversationOwnerType type
) {

    public static SchoolAssistantConversationOwner student(UUID studentId) {
        return new SchoolAssistantConversationOwner(
                studentId,
                SchoolAssistantConversationOwnerType.STUDENT
        );
    }

    public static SchoolAssistantConversationOwner user(UUID userId) {
        return new SchoolAssistantConversationOwner(
                userId,
                SchoolAssistantConversationOwnerType.USER
        );
    }

    public boolean isStudent() {
        return type == SchoolAssistantConversationOwnerType.STUDENT;
    }
}
