package com.recred.backend.application;

import java.util.function.Function;
import java.util.function.Predicate;

record SchoolAssistantFixedTopicRoute(
        Predicate<SchoolAssistantFixedTopicRequest> predicate,
        Function<SchoolAssistantFixedTopicRequest, SchoolAssistantFixedReply> reply
) implements SchoolAssistantFixedTopicHandler {

    @Override
    public boolean supports(SchoolAssistantFixedTopicRequest request) {
        return predicate.test(request);
    }

    @Override
    public SchoolAssistantFixedReply handle(SchoolAssistantFixedTopicRequest request) {
        return reply.apply(request);
    }
}