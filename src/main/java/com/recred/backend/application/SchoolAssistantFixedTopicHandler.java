package com.recred.backend.application;

interface SchoolAssistantFixedTopicHandler {

    boolean supports(SchoolAssistantFixedTopicRequest request);

    SchoolAssistantFixedReply handle(SchoolAssistantFixedTopicRequest request);
}
