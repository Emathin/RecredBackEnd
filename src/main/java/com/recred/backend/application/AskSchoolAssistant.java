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
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantRole;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
    private final SchoolAssistantMessageMetadataMapper metadataMapper;
    private final Map<SchoolAssistantRole, Function<AskSchoolAssistantRequest,
            SchoolAssistantAnswer>> answerByRole;
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
        this.metadataMapper = new SchoolAssistantMessageMetadataMapper();
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
        this.answerByRole = Map.of(
                SchoolAssistantRole.ALUMNO, this::studentAnswer,
                SchoolAssistantRole.TUTOR, this::tutorAnswer,
                SchoolAssistantRole.KIOSQUERO, this::kiosqueroAnswer
        );
        this.clock = clock;
    }

    public AskSchoolAssistantResponse execute(AskSchoolAssistantRequest request) {
        requireRequest(request);
        return AskSchoolAssistantResponse.from(answerFor(request));
    }

    private void requireRequest(AskSchoolAssistantRequest request) {
        if (request == null || request.role() == null) {
            throw new IllegalArgumentException("request and role are required");
        }
    }

    private SchoolAssistantAnswer answerFor(AskSchoolAssistantRequest request) {
        return answerResolver(request).apply(request);
    }

    private Function<AskSchoolAssistantRequest, SchoolAssistantAnswer> answerResolver(
            AskSchoolAssistantRequest request
    ) {
        Function<AskSchoolAssistantRequest, SchoolAssistantAnswer> resolver =
                answerByRole.get(request.role());

        if (resolver == null) {
            throw new IllegalArgumentException("unsupported school assistant role");
        }

        return resolver;
    }

    private SchoolAssistantAnswer studentAnswer(AskSchoolAssistantRequest request) {
        Usuario user = authenticatedUser(request.cognitoSubject());
        UUID studentId = resolvedStudentId(user.getId());
        String message = message(request);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = session(studentId, request, now);
        return answer(session, message, contextFactory.studentContext(studentId), now);
    }

    private SchoolAssistantAnswer tutorAnswer(AskSchoolAssistantRequest request) {
        Usuario user = authenticatedUser(request.cognitoSubject());
        Tutor tutor = tutor(request.cognitoSubject());
        String message = message(request);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), request, now);
        return answer(session, message, contextFactory.tutorContext(user.getId(), tutor), now);
    }

    private SchoolAssistantAnswer kiosqueroAnswer(AskSchoolAssistantRequest request) {
        Usuario user = authenticatedUser(request.cognitoSubject());
        UUID buffetId = buffetId(user.getId(), request.cognitoSubject());
        String message = message(request);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), request, now);
        SchoolAssistantContext context = contextFactory.kiosqueroContext(user.getId(), buffetId);
        return answer(session, message, context, now);
    }

    private SchoolAssistantAnswer answer(ChatSession session, String message,
                                         SchoolAssistantContext context,
                                         LocalDateTime now) {
        List<ChatMessage> recentMessages = recentMessages(session);
        Optional<SchoolAssistantFixedReply> fixedReply =
                fixedReply(message, context, recentMessages);
        SchoolAssistantAnswer answer = formattedAnswer(answerFrom(
                session,
                message,
                context,
                recentMessages,
                fixedReply,
                now
        ));
        saveConversation(session, message, context, answer, fixedReply, now);
        return answer;
    }

    private List<ChatMessage> recentMessages(ChatSession session) {
        return chatMessageRepository.findRecentBySessionId(session.id(), RECENT_HISTORY_LIMIT);
    }

    private Optional<SchoolAssistantFixedReply> fixedReply(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return fixedTopics.respond(message, context, recentMessages);
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

    private void saveConversation(
            ChatSession session,
            String message,
            SchoolAssistantContext context,
            SchoolAssistantAnswer answer,
            Optional<SchoolAssistantFixedReply> fixedReply,
            LocalDateTime now
    ) {
        saveUserMessage(session, message, context.capabilities(), now);
        saveAssistantMessage(answer, fixedReplyMetadata(fixedReply), now);
        updateSessionActivity(session, now);
    }

    private Map<String, Object> fixedReplyMetadata(
            Optional<SchoolAssistantFixedReply> fixedReply
    ) {
        return fixedReply.map(SchoolAssistantFixedReply::metadata).orElseGet(Map::of);
    }

    private void updateSessionActivity(ChatSession session, LocalDateTime now) {
        chatSessionRepository.updateLastActivity(session.id(), now);
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
            throw new KiosqueroNotFoundException(cognitoSubject);
        }

        return buffetId;
    }

    private String message(AskSchoolAssistantRequest request) {
        String message = request.message() == null ? "" : request.message().trim();

        if (message.isBlank()) {
            throw new InvalidChatMessageException();
        }

        return message;
    }

    private ChatSession session(
            UUID studentId,
            AskSchoolAssistantRequest request,
            LocalDateTime now
    ) {
        return request.optionalSessionId()
                .map(sessionId -> existingSession(studentId, sessionId))
                .orElseGet(() -> chatSessionRepository.startForStudent(studentId, now));
    }

    private ChatSession sessionForUser(
            UUID userId,
            AskSchoolAssistantRequest request,
            LocalDateTime now
    ) {
        return request.optionalSessionId()
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
                metadataMapper.userMetadata(capabilities),
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
                metadataMapper.answerMetadata(answer, additionalMetadata),
                now
        ));
    }
}
