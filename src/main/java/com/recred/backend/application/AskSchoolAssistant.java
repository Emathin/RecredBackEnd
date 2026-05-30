package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AskSchoolAssistant {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int RECENT_PURCHASE_LIMIT = 10;
    private static final int RECENT_HISTORY_LIMIT = 6;
    private static final int UPCOMING_EVENT_DAYS = 30;
    private static final List<SchoolAssistantCapability> ALLOWED_CAPABILITIES = List.of(
            SchoolAssistantCapability.SALDO,
            SchoolAssistantCapability.PAGOS,
            SchoolAssistantCapability.COMPRAS,
            SchoolAssistantCapability.EVENTOS
    );
    private static final String FALLBACK_ANSWER =
            "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                    + "y eventos escolares.";

    private final AlumnoRepository alumnoRepository;
    private final WalletRepository walletRepository;
    private final SchoolAssistantPaymentRepository paymentRepository;
    private final SchoolAssistantPurchaseRepository purchaseRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchoolAssistantResponder schoolAssistantResponder;
    private final Clock clock;

    public AskSchoolAssistant(
            AlumnoRepository alumnoRepository,
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository purchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SchoolAssistantResponder schoolAssistantResponder,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.schoolCalendarRepository = schoolCalendarRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.schoolAssistantResponder = schoolAssistantResponder;
        this.clock = clock;
    }

    public SchoolAssistantAnswer execute(UUID studentId, SchoolAssistantCommand command) {
        validateStudent(studentId);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = session(studentId, command, now);
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentBySessionId(
                session.id(),
                RECENT_HISTORY_LIMIT
        );
        SchoolAssistantContext context = context(studentId);
        SchoolAssistantPrompt prompt = new SchoolAssistantPrompt(
                message,
                context,
                recentMessages
        );
        Optional<SchoolAssistantDraft> draft = schoolAssistantResponder.respond(prompt)
                .filter(response -> response.answer() != null && !response.answer().isBlank());
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                session.id(),
                draft.map(SchoolAssistantDraft::answer).orElse(FALLBACK_ANSWER),
                ALLOWED_CAPABILITIES,
                now,
                draft.isPresent(),
                draft.map(SchoolAssistantDraft::model).orElse(null)
        );

        saveUserMessage(session, message, now);
        saveAssistantMessage(answer, now);
        chatSessionRepository.updateLastActivity(session.id(), now);

        return answer;
    }

    private void validateStudent(UUID studentId) {
        if (!alumnoRepository.existsById(studentId)) {
            throw new AlumnoNoEncontradoException(studentId);
        }
    }

    private String message(SchoolAssistantCommand command) {
        String message = command.message() == null ? "" : command.message().trim();

        if (message.isBlank()) {
            throw new InvalidChatMessageException();
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

    private ChatSession existingSession(UUID studentId, UUID sessionId) {
        return chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private SchoolAssistantContext context(UUID studentId) {
        LocalDate today = LocalDate.now(clock);
        return new SchoolAssistantContext(
                studentId,
                walletRepository.findByStudentId(studentId).map(Billetera::getSaldoCreditos),
                paymentRepository.findRecentByStudentId(studentId, RECENT_PAYMENT_LIMIT),
                purchaseRepository.findRecentByStudentId(studentId, RECENT_PURCHASE_LIMIT),
                schoolCalendarRepository.findByStudentIdAndDateRange(
                        studentId,
                        today,
                        today.plusDays(UPCOMING_EVENT_DAYS)
                )
        );
    }

    private void saveUserMessage(
            ChatSession session,
            String message,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                session.id(),
                RolChatMensaje.USUARIO,
                message,
                messageMetadata(),
                now
        ));
    }

    private void saveAssistantMessage(SchoolAssistantAnswer answer, LocalDateTime now) {
        chatMessageRepository.save(new ChatMessage(
                answer.sessionId(),
                RolChatMensaje.ASISTENTE_IA,
                answer.answer(),
                answerMetadata(answer),
                now
        ));
    }

    private Map<String, Object> messageMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capacidadesPermitidas", capabilityNames());
        return metadata;
    }

    private Map<String, Object> answerMetadata(SchoolAssistantAnswer answer) {
        Map<String, Object> metadata = messageMetadata();
        metadata.put("generadoPorIa", answer.generatedByAi());

        if (answer.model() != null) {
            metadata.put("modelo", answer.model());
        }

        return metadata;
    }

    private List<String> capabilityNames() {
        return ALLOWED_CAPABILITIES.stream()
                .map(SchoolAssistantCapability::name)
                .toList();
    }
}
