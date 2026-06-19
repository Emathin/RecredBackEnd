package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AskSchoolAssistant {

    private static final int RECENT_HISTORY_LIMIT = 6;

    private final AlumnoRepository alumnoRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchoolAssistantResponder schoolAssistantResponder;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final SchoolAssistantFixedTopics fixedTopics;
    private final SchoolAssistantAnswerFormatter answerFormatter;
    private final SchoolAssistantRoleCatalog roleCatalog;
    private final SchoolAssistantContextFactory contextFactory;
    private final Clock clock;

    public AskSchoolAssistant(
            AlumnoRepository alumnoRepository,
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository schoolAssistantPurchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SchoolAssistantResponder schoolAssistantResponder,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            KiosqueroRepository kiosqueroRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            PresupuestoRepository presupuestoRepository,
            SchoolAssistantFixedTopics fixedTopics,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.schoolAssistantResponder = schoolAssistantResponder;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.fixedTopics = fixedTopics;
        this.answerFormatter = new SchoolAssistantAnswerFormatter();
        this.roleCatalog = new SchoolAssistantRoleCatalog();
        this.contextFactory = new SchoolAssistantContextFactory(
                walletRepository,
                paymentRepository,
                schoolAssistantPurchaseRepository,
                schoolCalendarRepository,
                productRepository,
                purchaseRepository,
                presupuestoRepository,
                roleCatalog,
                clock
        );
        this.clock = clock;
    }

    public SchoolAssistantAnswer execute(UUID studentIdOrUserId, SchoolAssistantCommand command) {
        return executeForResolvedStudent(resolvedStudentId(studentIdOrUserId), command);
    }

    public SchoolAssistantAnswer executeForStudent(String cognitoSubject,
                                                   SchoolAssistantCommand command) {
        Usuario user = authenticatedUser(cognitoSubject);
        return executeForResolvedStudent(resolvedStudentId(user.getId()), command);
    }

    private SchoolAssistantAnswer executeForResolvedStudent(
            UUID studentId,
            SchoolAssistantCommand command
    ) {
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = session(studentId, command, now);
        return answer(session, message, contextFactory.studentContext(studentId), now);
    }

    public SchoolAssistantAnswer executeForTutor(String cognitoSubject,
                                                 SchoolAssistantCommand command) {
        Usuario user = authenticatedUser(cognitoSubject);
        Tutor tutor = tutor(cognitoSubject);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), command, now);
        return answer(session, message, contextFactory.tutorContext(user.getId(), tutor), now);
    }

    public SchoolAssistantAnswer executeForKiosquero(String cognitoSubject,
                                                     SchoolAssistantCommand command) {
        Usuario user = authenticatedUser(cognitoSubject);
        UUID buffetId = buffetId(user.getId(), cognitoSubject);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), command, now);
        return answer(
                session,
                message,
                contextFactory.kiosqueroContext(user.getId(), buffetId),
                now
        );
    }

    private SchoolAssistantAnswer answer(ChatSession session, String message,
                                         SchoolAssistantContext context,
                                         LocalDateTime now) {
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentBySessionId(
                session.id(),
                RECENT_HISTORY_LIMIT
        );
        Optional<SchoolAssistantFixedReply> fixedReply = fixedTopics.respond(
                message,
                context,
                recentMessages
        );
        SchoolAssistantAnswer rawAnswer = fixedReply
                .map(reply -> fixedAnswer(session, reply, context, now))
                .orElseGet(() -> aiAnswer(session, message, context, recentMessages, now));
        SchoolAssistantAnswer answer = formattedAnswer(rawAnswer);

        saveUserMessage(session, message, context.capabilities(), now);
        saveAssistantMessage(
                answer,
                fixedReply.map(SchoolAssistantFixedReply::metadata).orElseGet(Map::of),
                now
        );
        chatSessionRepository.updateLastActivity(session.id(), now);

        return answer;
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

    private UUID resolvedStudentId(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findStudentIdByUserId(studentIdOrUserId)
                        .orElseThrow(() -> new AlumnoNoEncontradoException(studentIdOrUserId));
    }

    private Usuario authenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private Tutor tutor(String cognitoSubject) {
        return tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new TutorNoEncontradoException(cognitoSubject));
    }

    private UUID buffetId(UUID userId, String cognitoSubject) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(userId);

        if (buffetId == null) {
            KiosqueroNotFoundException exception =
                    new KiosqueroNotFoundException(cognitoSubject);
            throw exception;
        }

        return buffetId;
    }

    private String message(SchoolAssistantCommand command) {
        String message = command.message() == null ? "" : command.message().trim();

        if (message.isBlank()) {
            InvalidChatMessageException exception = new InvalidChatMessageException();
            throw exception;
        }

        return message;
    }

    private ChatSession session(
            UUID studentId,
            SchoolAssistantCommand command,
            LocalDateTime now
    ) {
        return command.sessionId()
                .map(sessionId -> existingSession(studentId, sessionId))
                .orElseGet(() -> chatSessionRepository.startForStudent(studentId, now));
    }

    private ChatSession sessionForUser(
            UUID userId,
            SchoolAssistantCommand command,
            LocalDateTime now
    ) {
        return command.sessionId()
                .map(sessionId -> existingSessionForUser(userId, sessionId))
                .orElseGet(() -> chatSessionRepository.startForUser(userId, now));
    }

    private ChatSession existingSession(UUID studentId, UUID sessionId) {
        return chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private ChatSession existingSessionForUser(UUID userId, UUID sessionId) {
        return chatSessionRepository.findOpenByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private void saveUserMessage(
            ChatSession session,
            String message,
            List<SchoolAssistantCapability> capabilities,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                session.id(),
                RolChatMensaje.USUARIO,
                message,
                messageMetadata(capabilities),
                now
        ));
    }

    private void saveAssistantMessage(
            SchoolAssistantAnswer answer,
            Map<String, Object> additionalMetadata,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                answer.sessionId(),
                RolChatMensaje.ASISTENTE_IA,
                answer.answer(),
                answerMetadata(answer, additionalMetadata),
                now
        ));
    }

    private Map<String, Object> messageMetadata(List<SchoolAssistantCapability> capabilities) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capacidadesPermitidas", capabilityNames(capabilities));
        return metadata;
    }

    private Map<String, Object> answerMetadata(
            SchoolAssistantAnswer answer,
            Map<String, Object> additionalMetadata
    ) {
        Map<String, Object> metadata = messageMetadata(answer.capabilities());
        metadata.put("generadoPorIa", answer.generatedByAi());
        metadata.put("rol", value(answer.role()));
        metadata.put("intencion", answer.intent().name());
        metadata.put("accion", actionMetadata(answer.action()));
        metadata.put("sugerencias", suggestionsMetadata(answer.suggestions()));
        metadata.put("datos", answer.data());
        metadata.putAll(additionalMetadata);

        if (answer.model() != null) {
            String model = answer.model();
            metadata.put("modelo", model);
        }

        return metadata;
    }

    private List<String> capabilityNames(List<SchoolAssistantCapability> capabilities) {
        return capabilities.stream()
                .map(SchoolAssistantCapability::name)
                .toList();
    }

    private Map<String, Object> actionMetadata(SchoolAssistantAction action) {
        if (action == null) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tipo", action.type().name());
        metadata.put("estado", action.status().name());
        metadata.put("requiereInput", action.requiresInput());
        metadata.put("inputsRequeridos", action.requiredInputs().stream()
                .map(Enum::name)
                .toList());
        metadata.put("compraId", value(action.purchaseId()));
        metadata.put("estadoCompra", value(action.purchaseStatus()));
        metadata.put("codigoRetiro", value(action.withdrawalCode()));
        metadata.put("total", value(action.total()));
        metadata.put("recreo", value(action.recessTime()));
        metadata.put("datos", action.data());
        return metadata;
    }

    private List<Map<String, Object>> suggestionsMetadata(
            List<SchoolAssistantSuggestion> suggestions
    ) {
        return suggestions.stream()
                .map(this::suggestionMetadata)
                .toList();
    }

    private Map<String, Object> suggestionMetadata(SchoolAssistantSuggestion suggestion) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("label", suggestion.label());
        metadata.put("mensaje", suggestion.message());
        metadata.put("tipoAccion", suggestion.actionType().name());
        return metadata;
    }

    private String value(Object value) {
        return value == null ? "sin_datos" : value.toString();
    }
}
