package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantQueryInterpreter;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AskSchoolAssistant {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int RECENT_HISTORY_LIMIT = 6;
    private static final int UPCOMING_EVENT_DAYS = 7;
    private static final int MONEY_SCALE = 2;
    private static final String MONEY_PREFIX = "$";
    private static final String OUT_OF_SCOPE_ANSWER =
            "Puedo ayudarte con consultas de saldo, pagos y eventos escolares.";
    private static final String MISSING_WALLET_ANSWER =
            "No encontre una billetera asociada al alumno.";
    private static final String EMPTY_PAYMENTS_ANSWER =
            "No hay pagos recientes registrados.";
    private static final String EMPTY_EVENTS_ANSWER =
            "No hay eventos escolares proximos registrados.";

    private final AlumnoRepository alumnoRepository;
    private final WalletRepository walletRepository;
    private final SchoolAssistantPaymentRepository paymentRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchoolAssistantQueryInterpreter schoolAssistantQueryInterpreter;
    private final SchoolAssistantResponder schoolAssistantResponder;
    private final Clock clock;

    public AskSchoolAssistant(
            AlumnoRepository alumnoRepository,
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SchoolAssistantQueryInterpreter schoolAssistantQueryInterpreter,
            SchoolAssistantResponder schoolAssistantResponder,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.schoolCalendarRepository = schoolCalendarRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.schoolAssistantQueryInterpreter = schoolAssistantQueryInterpreter;
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
        SchoolAssistantQuery query = query(message, LocalDate.now(clock), recentMessages);

        saveUserMessage(session, message, query, now);

        SchoolAssistantContext context = context(studentId, query);
        SchoolAssistantPrompt prompt = new SchoolAssistantPrompt(
                message,
                query,
                context,
                recentMessages
        );
        Optional<SchoolAssistantDraft> draft = draft(prompt);
        String answerText = draft
                .map(SchoolAssistantDraft::answer)
                .orElseGet(() -> fallbackAnswer(prompt));
        SchoolAssistantAnswer answer = new SchoolAssistantAnswer(
                session.id(),
                answerText,
                query.capabilities(),
                now,
                draft.isPresent(),
                draft.map(SchoolAssistantDraft::model).orElse(null)
        );

        saveAssistantMessage(answer, now);
        chatSessionRepository.updateLastActivity(session.id(), now);

        return answer;
    }

    private SchoolAssistantQuery query(
            String message,
            LocalDate today,
            List<ChatMessage> recentMessages
    ) {
        return schoolAssistantQueryInterpreter.interpret(message, today, recentMessages)
                .orElseGet(this::outOfScopeQuery);
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

    private void saveUserMessage(
            ChatSession session,
            String message,
            SchoolAssistantQuery query,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                session.id(),
                RolChatMensaje.USUARIO,
                message,
                messageMetadata(query),
                now
        ));
    }

    private SchoolAssistantContext context(
            UUID studentId,
            SchoolAssistantQuery query
    ) {
        LocalDate today = LocalDate.now(clock);
        return new SchoolAssistantContext(
                studentId,
                balance(studentId, query.capabilities()),
                payments(studentId, query),
                events(studentId, query, today)
        );
    }

    private Optional<BigDecimal> balance(
            UUID studentId,
            List<SchoolAssistantCapability> capabilities
    ) {
        return hasCapability(capabilities, SchoolAssistantCapability.SALDO)
                ? walletRepository.findByStudentId(studentId).map(Billetera::getSaldoCreditos)
                : Optional.empty();
    }

    private List<SchoolAssistantPayment> payments(
            UUID studentId,
            SchoolAssistantQuery query
    ) {
        if (!hasCapability(query.capabilities(), SchoolAssistantCapability.PAGOS)) {
            return List.of();
        }

        return query.paymentDateRange()
                .map(range -> paymentRepository.findByStudentIdAndDateRange(
                        studentId,
                        range.startDate().atStartOfDay(),
                        range.endDate().atTime(LocalTime.MAX),
                        query.paymentLimit()
                ))
                .orElseGet(() -> paymentRepository.findRecentByStudentId(
                        studentId,
                        query.paymentLimit()
                ));
    }

    private List<SchoolCalendarEvent> events(
            UUID studentId,
            SchoolAssistantQuery query,
            LocalDate today
    ) {
        DateRange eventDateRange = query.eventDateRange()
                .orElseGet(() -> defaultUpcomingEventRange(today));

        return hasCapability(query.capabilities(), SchoolAssistantCapability.EVENTOS)
                ? schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                eventDateRange.startDate(),
                eventDateRange.endDate()
        )
                : List.of();
    }

    private Optional<SchoolAssistantDraft> draft(SchoolAssistantPrompt prompt) {
        return !prompt.query().requiresAiResponse()
                || isOutOfScope(prompt.query().capabilities())
                ? Optional.empty()
                : schoolAssistantResponder.respond(prompt)
                .filter(draft -> draft.answer() != null && !draft.answer().isBlank());
    }

    private String fallbackAnswer(SchoolAssistantPrompt prompt) {
        return isOutOfScope(prompt.query().capabilities())
                ? OUT_OF_SCOPE_ANSWER
                : prompt.query().capabilities().stream()
                .map(capability -> localAnswerForCapability(capability, prompt.context()))
                .collect(Collectors.joining(" "));
    }

    private String localAnswerForCapability(
            SchoolAssistantCapability capability,
            SchoolAssistantContext context
    ) {
        return switch (capability) {
            case SALDO -> balanceFallback(context);
            case PAGOS -> paymentFallback(context);
            case EVENTOS -> eventFallback(context);
            case FUERA_DE_ALCANCE -> OUT_OF_SCOPE_ANSWER;
        };
    }

    private String balanceFallback(SchoolAssistantContext context) {
        return context.balance()
                .map(balance -> "Saldo disponible: " + amount(balance) + ".")
                .orElse(MISSING_WALLET_ANSWER);
    }

    private String paymentFallback(SchoolAssistantContext context) {
        return context.recentPayments().isEmpty()
                ? EMPTY_PAYMENTS_ANSWER
                : latestPaymentFallback(context.recentPayments().get(0));
    }

    private String latestPaymentFallback(SchoolAssistantPayment payment) {
        return "Ultimo pago: "
                + amount(payment.amount())
                + " "
                + payment.status().name()
                + " el "
                + payment.createdAt().toLocalDate()
                + ".";
    }

    private String eventFallback(SchoolAssistantContext context) {
        return context.upcomingEvents().isEmpty()
                ? EMPTY_EVENTS_ANSWER
                : latestEventFallback(context.upcomingEvents().get(0));
    }

    private String latestEventFallback(SchoolCalendarEvent event) {
        return "Proximo evento: "
                + event.date()
                + " - "
                + event.description()
                + ".";
    }

    private String amount(BigDecimal amount) {
        return MONEY_PREFIX + amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean hasCapability(
            List<SchoolAssistantCapability> capabilities,
            SchoolAssistantCapability capability
    ) {
        return capabilities.contains(capability);
    }

    private boolean isOutOfScope(List<SchoolAssistantCapability> capabilities) {
        return capabilities.contains(SchoolAssistantCapability.FUERA_DE_ALCANCE);
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

    private Map<String, Object> messageMetadata(SchoolAssistantQuery query) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capacidades", capabilityNames(query.capabilities()));
        metadata.put("limitePagos", query.paymentLimit());
        metadata.put("requiereRedaccionIa", query.requiresAiResponse());
        query.paymentDateRange().ifPresent(range -> metadata.put("rangoPagos", range(range)));
        query.eventDateRange().ifPresent(range -> metadata.put("rangoEventos", range(range)));
        return metadata;
    }

    private Map<String, Object> answerMetadata(SchoolAssistantAnswer answer) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capacidades", capabilityNames(answer.capabilities()));
        metadata.put("generadoPorIa", answer.generatedByAi());

        if (answer.model() != null) {
            metadata.put("modelo", answer.model());
        }

        return metadata;
    }

    private Map<String, Object> range(DateRange range) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("desde", range.startDate().toString());
        metadata.put("hasta", range.endDate().toString());
        return metadata;
    }

    private DateRange defaultUpcomingEventRange(LocalDate today) {
        return new DateRange(today, today.plusDays(UPCOMING_EVENT_DAYS));
    }

    private SchoolAssistantQuery outOfScopeQuery() {
        return new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                Optional.empty(),
                Optional.empty(),
                RECENT_PAYMENT_LIMIT,
                false
        );
    }

    private List<String> capabilityNames(List<SchoolAssistantCapability> capabilities) {
        return capabilities.stream()
                .map(SchoolAssistantCapability::name)
                .toList();
    }
}
