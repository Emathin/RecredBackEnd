package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AskSchoolAssistant {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int CHILD_PAYMENT_LIMIT = 2;
    private static final int RECENT_PURCHASE_LIMIT = 10;
    private static final int RECENT_HISTORY_LIMIT = 6;
    private static final int UPCOMING_EVENT_DAYS = 30;
    private static final int PRODUCT_FACT_LIMIT = 20;
    private static final int ORDER_FACT_LIMIT = 10;
    private static final List<SchoolAssistantCapability> STUDENT_CAPABILITIES = List.of(
            SchoolAssistantCapability.SALDO,
            SchoolAssistantCapability.PAGOS,
            SchoolAssistantCapability.COMPRAS,
            SchoolAssistantCapability.EVENTOS
    );
    private static final List<SchoolAssistantCapability> TUTOR_CAPABILITIES = List.of(
            SchoolAssistantCapability.HIJOS,
            SchoolAssistantCapability.SALDO,
            SchoolAssistantCapability.PAGOS,
            SchoolAssistantCapability.COMPRAS,
            SchoolAssistantCapability.EVENTOS,
            SchoolAssistantCapability.PRESUPUESTOS,
            SchoolAssistantCapability.RESTRICCIONES
    );
    private static final List<SchoolAssistantCapability> KIOSQUERO_CAPABILITIES = List.of(
            SchoolAssistantCapability.STOCK,
            SchoolAssistantCapability.VENTAS,
            SchoolAssistantCapability.PEDIDOS,
            SchoolAssistantCapability.COMPRAS
    );
    private static final String STUDENT_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                    + "y eventos escolares.";
    private static final String TUTOR_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras, "
                    + "eventos, presupuestos y restricciones de tus hijos.";
    private static final String KIOSQUERO_FALLBACK =
            "No pude responder ahora. Puedo ayudarte con stock, ventas y pedidos del buffet.";

    private final AlumnoRepository alumnoRepository;
    private final WalletRepository walletRepository;
    private final SchoolAssistantPaymentRepository paymentRepository;
    private final SchoolAssistantPurchaseRepository schoolAssistantPurchaseRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchoolAssistantResponder schoolAssistantResponder;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final SchoolAssistantFixedTopics fixedTopics;
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
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.schoolAssistantPurchaseRepository = schoolAssistantPurchaseRepository;
        this.schoolCalendarRepository = schoolCalendarRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.schoolAssistantResponder = schoolAssistantResponder;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.fixedTopics = fixedTopics;
        this.clock = clock;
    }

    public SchoolAssistantAnswer execute(UUID studentIdOrUserId, SchoolAssistantCommand command) {
        UUID studentId = resolvedStudentId(studentIdOrUserId);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = session(studentId, command, now);
        return answer(session, message, studentContext(studentId), now);
    }

    public SchoolAssistantAnswer executeForTutor(String cognitoSubject,
                                                 SchoolAssistantCommand command) {
        Usuario user = authenticatedUser(cognitoSubject);
        Tutor tutor = tutor(cognitoSubject);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), command, now);
        return answer(session, message, tutorContext(user.getId(), tutor), now);
    }

    public SchoolAssistantAnswer executeForKiosquero(String cognitoSubject,
                                                     SchoolAssistantCommand command) {
        Usuario user = authenticatedUser(cognitoSubject);
        UUID buffetId = buffetId(user.getId(), cognitoSubject);
        String message = message(command);
        LocalDateTime now = LocalDateTime.now(clock);
        ChatSession session = sessionForUser(user.getId(), command, now);
        return answer(session, message, kiosqueroContext(user.getId(), buffetId), now);
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
        SchoolAssistantAnswer answer = fixedReply
                .map(reply -> fixedAnswer(session, reply, context, now))
                .orElseGet(() -> aiAnswer(session, message, context, recentMessages, now));

        saveUserMessage(session, message, context.capabilities(), now);
        saveAssistantMessage(
                answer,
                fixedReply.map(SchoolAssistantFixedReply::metadata).orElseGet(Map::of),
                now
        );
        chatSessionRepository.updateLastActivity(session.id(), now);

        return answer;
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
                draft.map(SchoolAssistantDraft::answer).orElse(fallbackAnswer(context.role())),
                context.capabilities(),
                now,
                draft.isPresent(),
                draft.map(SchoolAssistantDraft::model).orElse(null)
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
                null
        );
    }

    private UUID resolvedStudentId(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findIdByUsuarioId(studentIdOrUserId)
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

    private SchoolAssistantContext studentContext(UUID studentId) {
        LocalDate today = LocalDate.now(clock);
        return new SchoolAssistantContext(
                studentId,
                walletRepository.findByStudentId(studentId).map(Billetera::getSaldoCreditos),
                paymentRepository.findRecentByStudentId(studentId, RECENT_PAYMENT_LIMIT),
                schoolAssistantPurchaseRepository.findRecentByStudentId(
                        studentId,
                        RECENT_PURCHASE_LIMIT
                ),
                schoolCalendarRepository.findByStudentIdAndDateRange(
                        studentId,
                        today,
                        today.plusDays(UPCOMING_EVENT_DAYS)
                ),
                SchoolAssistantRole.ALUMNO,
                null,
                null,
                List.of(),
                STUDENT_CAPABILITIES
        );
    }

    private SchoolAssistantContext tutorContext(UUID userId, Tutor tutor) {
        List<Alumno> children = children(tutor);
        List<Purchase> purchases = purchaseRepository.findHistorialByTutor(tutor.getId());
        List<String> facts = new ArrayList<>();
        facts.add("tutorId=" + value(tutor.getId()));
        children.stream()
                .map(this::childFact)
                .forEach(facts::add);
        purchases.stream()
                .limit(RECENT_PURCHASE_LIMIT)
                .map(this::purchaseFact)
                .map(fact -> "compraTutor=" + fact)
                .forEach(facts::add);

        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                tutorPayments(children),
                purchases.stream()
                        .limit(RECENT_PURCHASE_LIMIT)
                        .map(this::assistantPurchase)
                        .toList(),
                tutorEvents(children),
                SchoolAssistantRole.TUTOR,
                userId,
                null,
                facts,
                TUTOR_CAPABILITIES
        );
    }

    private SchoolAssistantContext kiosqueroContext(UUID userId, UUID buffetId) {
        List<String> facts = new ArrayList<>();
        facts.add("buffetId=" + buffetId);
        productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .limit(PRODUCT_FACT_LIMIT)
                .map(this::productFact)
                .map(fact -> "productoBuffet=" + fact)
                .forEach(facts::add);
        List<Purchase> pendingOrders = purchaseRepository.findByBuffetAndStatuses(
                buffetId,
                List.of(EstadoCompra.PENDIENTE, EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO),
                ORDER_FACT_LIMIT
        );
        facts.add("pedidosPendientes=" + pendingOrders.size());
        pendingOrders.stream()
                .map(this::purchaseFact)
                .map(fact -> "pedido=" + fact)
                .forEach(facts::add);

        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                purchaseRepository.findRecentByBuffet(buffetId, RECENT_PURCHASE_LIMIT)
                        .stream()
                        .map(this::assistantPurchase)
                        .toList(),
                List.of(),
                SchoolAssistantRole.KIOSQUERO,
                userId,
                buffetId,
                facts,
                KIOSQUERO_CAPABILITIES
        );
    }

    private List<Alumno> children(Tutor tutor) {
        Set<Alumno> children = tutor.getHijos();

        if (children == null) {
            List<Alumno> emptyChildren = List.of();
            return emptyChildren;
        }

        return children.stream().toList();
    }

    private String childFact(Alumno child) {
        return "hijo=" + name(child)
                + "|id=" + value(child.getId())
                + "|saldo=" + balance(child)
                + "|grado=" + grade(child)
                + "|presupuesto=" + budget(child)
                + "|restriccionesSalud=" + healthRestrictions(child)
                + "|productosBloqueados=" + blockedProducts(child);
    }

    private String productFact(Producto product) {
        return "id=" + value(product.getId())
                + "|nombre=" + value(product.getNombre())
                + "|precio=" + value(product.getPrecio())
                + "|stock=" + value(product.getStockActual())
                + "|requierePreparacion=" + value(product.getRequierePreparacion());
    }

    private String purchaseFact(Purchase purchase) {
        return "id=" + value(purchase.getId())
                + "|alumnoId=" + studentId(purchase)
                + "|monto=" + value(purchase.getTotalAmount())
                + "|estado=" + value(purchase.getStatus())
                + "|medioPago=" + value(purchase.getPaymentMethod())
                + "|fecha=" + value(purchase.getDate());
    }

    private SchoolAssistantPurchase assistantPurchase(Purchase purchase) {
        return new SchoolAssistantPurchase(
                purchase.getTotalAmount(),
                purchase.getStatus(),
                purchase.getPaymentMethod(),
                purchase.getDate()
        );
    }

    private List<SchoolCalendarEvent> tutorEvents(List<Alumno> children) {
        LocalDate today = LocalDate.now(clock);
        return children.stream()
                .flatMap(child -> schoolCalendarRepository.findByStudentIdAndDateRange(
                                child.getId(),
                                today,
                                today.plusDays(UPCOMING_EVENT_DAYS)
                        )
                        .stream())
                .toList();
    }

    private List<SchoolAssistantPayment> tutorPayments(List<Alumno> children) {
        return children.stream()
                .flatMap(child -> paymentRepository.findRecentByStudentId(
                                child.getId(),
                                CHILD_PAYMENT_LIMIT
                        )
                        .stream())
                .limit(RECENT_PAYMENT_LIMIT)
                .toList();
    }

    private String budget(Alumno child) {
        return presupuestoRepository.findActivoByAlumnoId(child.getId())
                .map(this::budgetFact)
                .orElse("sin_presupuesto");
    }

    private String budgetFact(PresupuestoAlumno budget) {
        return "monto=" + value(budget.getMontoLimiteGeneral())
                + ",periodo=" + value(budget.getPeriodo())
                + ",fechaInicio=" + value(budget.getFechaInicio());
    }

    private String name(Alumno child) {
        if (child.getUsuario() == null) {
            String missingName = "sin_datos";
            return missingName;
        }

        return value(child.getUsuario().getNombre()) + " "
                + value(child.getUsuario().getApellido());
    }

    private String balance(Alumno child) {
        return Optional.ofNullable(child.getBilletera())
                .map(Billetera::getSaldoCreditos)
                .map(BigDecimal::toString)
                .orElse("sin_datos");
    }

    private String grade(Alumno child) {
        if (child.getGrado() == null) {
            String missingGrade = "sin_datos";
            return missingGrade;
        }

        return value(child.getGrado().getAnio()) + " "
                + value(child.getGrado().getDivision());
    }

    private String healthRestrictions(Alumno child) {
        if (child.getRestriccionesSalud() == null) {
            String emptyRestrictions = "[]";
            return emptyRestrictions;
        }

        return child.getRestriccionesSalud()
                .stream()
                .map(restriction -> value(restriction.getDescripcion()))
                .toList()
                .toString();
    }

    private String blockedProducts(Alumno child) {
        if (child.getProductosBloqueados() == null) {
            String emptyProducts = "[]";
            return emptyProducts;
        }

        return child.getProductosBloqueados()
                .stream()
                .map(product -> value(product.getNombre()))
                .toList()
                .toString();
    }

    private String studentId(Purchase purchase) {
        if (purchase.getStudent() == null) {
            String missingStudent = "sin_datos";
            return missingStudent;
        }

        return value(purchase.getStudent().getId());
    }

    private String fallbackAnswer(SchoolAssistantRole role) {
        return switch (role) {
            case TUTOR -> TUTOR_FALLBACK;
            case KIOSQUERO -> KIOSQUERO_FALLBACK;
            case ALUMNO -> STUDENT_FALLBACK;
        };
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

    private String value(Object value) {
        return value == null ? "sin_datos" : value.toString();
    }
}
