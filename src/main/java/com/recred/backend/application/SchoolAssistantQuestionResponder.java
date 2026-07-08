package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchoolAssistantQuestionResponder {

    private final SchoolAssistantFixedTopics fixedTopics;
    private final SchoolAssistantResponder schoolAssistantResponder;
    private final SchoolAssistantAnswerFormatter answerFormatter;
    private final SchoolAssistantRoleCatalog roleCatalog;

    public SchoolAssistantQuestionResponder(
            SchoolAssistantFixedTopics fixedTopics,
            SchoolAssistantResponder schoolAssistantResponder,
            SchoolAssistantAnswerFormatter answerFormatter,
            SchoolAssistantRoleCatalog roleCatalog
    ) {
        this.fixedTopics = fixedTopics;
        this.schoolAssistantResponder = schoolAssistantResponder;
        this.answerFormatter = answerFormatter;
        this.roleCatalog = roleCatalog;
    }

    public SchoolAssistantAnswerResult respond(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        List<ChatMessage> recentMessages = conversation.recentMessages();
        Optional<SchoolAssistantFixedReply> fixedReply =
                fixedTopics.respond(message, context, recentMessages);
        SchoolAssistantAnswer answer = formattedAnswer(answerFrom(
                conversation.session(),
                message,
                context,
                recentMessages,
                fixedReply,
                now
        ));
        return new SchoolAssistantAnswerResult(answer, fixedReplyMetadata(fixedReply));
    }

    private SchoolAssistantAnswer answerFrom(
            ChatSession session,
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            Optional<SchoolAssistantFixedReply> fixedReply,
            LocalDateTime now
    ) {
        return fixedReply
                .map(reply -> fixedAnswer(session, reply, context, now))
                .orElseGet(() -> aiAnswer(session, message, context, recentMessages, now));
    }

    private Map<String, Object> fixedReplyMetadata(
            Optional<SchoolAssistantFixedReply> fixedReply
    ) {
        return fixedReply.map(SchoolAssistantFixedReply::metadata).orElseGet(Map::of);
    }

    private SchoolAssistantAnswer formattedAnswer(SchoolAssistantAnswer answer) {
        String formattedAnswer = answerFormatter.format(answer.answer());
        return formattedAnswer.equals(answer.answer())
                ? answer
                : new SchoolAssistantAnswer(
                        answer.sessionId(),
                        formattedAnswer,
                        answer.capabilities(),
                        answer.dateTime(),
                        answer.generatedByAi(),
                        answer.model(),
                        answer.role(),
                        answer.intent(),
                        answer.action(),
                        answer.suggestions(),
                        answer.data()
                );
    }

    private SchoolAssistantAnswer aiAnswer(
            ChatSession session,
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            LocalDateTime now
    ) {
        SchoolAssistantPrompt prompt = new SchoolAssistantPrompt(message, context, recentMessages);
        Optional<SchoolAssistantDraft> draft = schoolAssistantResponder.respond(prompt)
                .filter(response -> response.answer() != null && !response.answer().isBlank());
        return new SchoolAssistantAnswer(
                session.id(),
                draft.map(SchoolAssistantDraft::answer)
                        .orElse(roleCatalog.fallbackAnswer(context.role())),
                context.capabilities(),
                now,
                draft.isPresent(),
                draft.map(SchoolAssistantDraft::model).orElse(null),
                context.role(),
                SchoolAssistantIntent.UNKNOWN,
                null,
                roleCatalog.defaultSuggestions(context.role()),
                Map.of()
        );
    }

    private SchoolAssistantAnswer fixedAnswer(
            ChatSession session,
            SchoolAssistantFixedReply reply,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return new SchoolAssistantAnswer(
                session.id(),
                reply.answer(),
                context.capabilities(),
                now,
                false,
                null,
                context.role(),
                reply.intent(),
                reply.action(),
                reply.suggestions(),
                reply.data()
        );
    }
}
