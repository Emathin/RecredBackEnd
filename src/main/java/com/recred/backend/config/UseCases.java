package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.BloquearProductoManual;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DeleteCarritoFavorito;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DesbloquearProductoManual;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.GenerarMenuBuffet;
import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetActivePromotions;

import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetBuffetByAlumno;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetCarritosFavoritosByTutor;
import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetFavoritosByAlumno;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.ManagePromotionStatus;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.ObtenerHijosPorPadre;
import com.recred.backend.application.ObtenerHistorialAlumno;
import com.recred.backend.application.ObtenerHistorialPendienteAlumno;
import com.recred.backend.application.ObtenerHistorialTutor;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PrizeHealthyPurchase;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.SaveCarritoFavorito;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UploadProductImage;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProductStatisticsRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import com.recred.backend.domain.services.ExternalPaymentService;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.PurchaseValidatorImpl;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import com.recred.backend.domain.services.TutorService;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.domain.services.WithdrawalCodeServiceImpl;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.gemini.GeminiBulkStockAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiParentSummaryAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCases {

    @Bean
    public GenerateComboSuggestion generateComboSuggestion(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository) {
        return new GenerateComboSuggestion(productRepository, kiosqueroRepository);
    }

    @Bean
    public GenerateParentSummary generateParentSummary(
            TutorRepository tutorRepository,
            PurchaseRepository purchaseRepository,
            SummaryRepository summaryRepository,
            PresupuestoRepository presupuestoRepository,
            ParentSummaryAnalyzer parentSummaryAnalyzer) {
        return new GenerateParentSummary(tutorRepository,
                purchaseRepository, summaryRepository,
                presupuestoRepository, parentSummaryAnalyzer);
    }

    @Bean
    public ParentSummaryAnalyzer parentSummaryAnalyzer(GeminiTextClient geminiTextClient) {
        return new GeminiParentSummaryAnalyzer(geminiTextClient);
    }

    @Bean
    public WithdrawalCodeServiceImpl withdrawalCodeServiceImpl() {
        return new WithdrawalCodeServiceImpl();
    }

    @Bean
    public PurchaseValidatorImpl purchaseValidatorImpl(
            ValidadorHorarioService validadorHorarioService,
            ValidacionRestriccionesService validacionRestriccionesService,
            ValidadorPresupuestoService validadorPresupuestoService) {
        return new PurchaseValidatorImpl(validadorHorarioService,
                validacionRestriccionesService, validadorPresupuestoService);
    }

    @Bean
    public LoadStockIAService loadStockIAService(GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper) {
        return new LoadStockIAService(geminiTextClient, objectMapper);
    }

    @Bean
    public GeneratePriceAlert generatePriceAlert(
            StudentAIProfileRepository studentAIProfileRepository,
            PriceAlertAIRepository priceAlertAIRepository,
            SugerenciaIARepository sugerenciaIARepository,
            Clock clock) {
        return new GeneratePriceAlert(studentAIProfileRepository,
                priceAlertAIRepository, sugerenciaIARepository, clock);
    }

    @Bean
    public UpdateProductPrice updateProductPrice(
            ProductRepository productRepository,
            ProductPriceChangedPublisher eventPublisher) {
        return new UpdateProductPrice(productRepository, eventPublisher);
    }

    @Bean
    public PurchaseValidator purchaseValidator(
            ValidadorHorarioService validadorHorarioService,
            ValidacionRestriccionesService validacionRestriccionesService,
            ValidadorPresupuestoService validadorPresupuestoService) {
        return new PurchaseValidatorImpl(
                validadorHorarioService,
                validacionRestriccionesService,
                validadorPresupuestoService);
    }

    @Bean
    public WithdrawalCodeService withdrawalCodeService() {
        return new WithdrawalCodeServiceImpl();
    }

    @Bean
    public CreateAdvancePurchase createAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            InventoryAvailabilityService inventoryAvailabilityService,
            RecessTimeSlotResolver recessTimeSlotResolver,
            AdvancePurchaseRulesService advancePurchaseRulesService,
            WalletRepository walletRepository,
            SpringDataBuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new CreateAdvancePurchase(
                purchaseValidator,
                alumnoRepository,
                productRepository,
                purchaseRepository,
                withdrawalCodeService,
                inventoryAvailabilityService,
                recessTimeSlotResolver,
                advancePurchaseRulesService,
                walletRepository,
                buffetRepository,
                usuarioRepository,
                tutorRepository,
                movimientoBilleteraRepository);
    }

    @Bean
    public RecessTimeSlotResolver recessTimeSlotResolver(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new RecessTimeSlotResolver(franjaHorariaRepository);
    }

    @Bean
    public AdvancePurchaseRulesService advancePurchaseRulesService(
            PurchaseRepository purchaseRepository,
            Clock clock,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher) {
        return new AdvancePurchaseRulesService(
                purchaseRepository,
                clock,
                realtimeEventPublisher);
    }

    @Bean
    public CalculadorPeriodoPresupuesto calculadorPeriodoPresupuesto() {
        return new CalculadorPeriodoPresupuesto();
    }

    @Bean
    public ValidacionRestriccionesService validacionRestriccionesService() {
        return new ValidacionRestriccionesService();
    }

    @Bean
    public BloquearProductoManual bloquearProductoManual(AlumnoRepository alumnoRepository,
            ProductRepository productRepository) {
        return new BloquearProductoManual(alumnoRepository, productRepository);
    }

    @Bean
    public DesbloquearProductoManual desbloquearProductoManual(AlumnoRepository alumnoRepository) {
        return new DesbloquearProductoManual(alumnoRepository);
    }

    @Bean
    public AgregarProductoFavorito agregarProductoFavorito(AlumnoRepository alumnoRepository,
            ProductRepository productRepository) {
        return new AgregarProductoFavorito(alumnoRepository, productRepository);
    }

    @Bean
    public RemoverProductoFavorito removerProductoFavorito(AlumnoRepository alumnoRepository) {
        return new RemoverProductoFavorito(alumnoRepository);
    }

    @Bean
    public GetFavoritosByAlumno getFavoritosByAlumno(AlumnoRepository alumnoRepository) {
        return new GetFavoritosByAlumno(alumnoRepository);
    }

    @Bean
    public UpdateBudgetRules updateBudgetRules(PresupuestoRepository repository) {
        return new UpdateBudgetRules(repository);
    }

    @Bean
    public ObtenerHijosPorPadre obtenerHijosPorPadre(TutorRepository tutorRepository) {
        return new ObtenerHijosPorPadre(tutorRepository);
    }

    @Bean
    public CreateStudentFromParent createStudentFromParent(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            GradoRepository gradoRepository,
            CognitoStudentIdentityService cognitoStudentIdentityService) {
        return new CreateStudentFromParent(
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                gradoRepository,
                cognitoStudentIdentityService);
    }

    @Bean
    public ValidadorHorarioService validadorHorarioService() {
        return new ValidadorHorarioService();
    }

    @Bean
    public ValidadorPresupuestoService validadorPresupuestoService(
            PresupuestoRepository presupuestoRepository,
            PurchaseRepository purchaseRepository,
            CalculadorPeriodoPresupuesto calculadorPeriodo) {
        return new ValidadorPresupuestoService(
                presupuestoRepository,
                purchaseRepository,
                calculadorPeriodo);
    }

    @Bean
    public UsuarioService usuarioService(UsuarioRepository usuarioRepository) {
        return new UsuarioService(usuarioRepository);
    }

    @Bean
    public TutorService tutorService(TutorRepository tutorRepository) {
        return new TutorService(tutorRepository);
    }

    @Bean
    public SchoolAssistantFixedTopics schoolAssistantFixedTopics(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            Clock clock) {
        return new SchoolAssistantFixedTopics(
                purchaseRepository,
                productRepository,
                createAssistantAdvancePurchase,
                clock);
    }

    @Bean
    public AskSchoolAssistant askSchoolAssistant(
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
            Clock clock) {
        return new AskSchoolAssistant(
                alumnoRepository,
                walletRepository,
                paymentRepository,
                schoolAssistantPurchaseRepository,
                schoolCalendarRepository,
                chatSessionRepository,
                chatMessageRepository,
                schoolAssistantResponder,
                usuarioRepository,
                tutorRepository,
                kiosqueroRepository,
                productRepository,
                purchaseRepository,
                presupuestoRepository,
                fixedTopics,
                clock);
    }

    @Bean
    public SchoolAssistantConversations schoolAssistantConversations(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AlumnoRepository alumnoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            KiosqueroRepository kiosqueroRepository,
            Clock clock) {
        return new SchoolAssistantConversations(
                chatSessionRepository,
                chatMessageRepository,
                alumnoRepository,
                usuarioRepository,
                tutorRepository,
                kiosqueroRepository,
                clock);
    }

    @Bean
    public GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail(
            ConsumptionAccessRepository consumptionAccessRepository,
            StudentConsumptionRepository studentConsumptionRepository,
            Clock clock) {
        return new GetLoggedUserConsumptionDetail(
                consumptionAccessRepository,
                studentConsumptionRepository,
                clock);
    }

    @Bean
    public GetStudentWalletSummary getStudentWalletSummary(
            ConsumptionAccessRepository consumptionAccessRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            StudentConsumptionRepository studentConsumptionRepository,
            Clock clock) {
        return new GetStudentWalletSummary(
                consumptionAccessRepository,
                walletRepository,
                movimientoBilleteraRepository,
                studentConsumptionRepository,
                clock);
    }

    @Bean
    public GetLoggedUserNotifications getLoggedUserNotifications(
            UsuarioRepository usuarioRepository,
            NotificationRepository notificationRepository) {
        return new GetLoggedUserNotifications(usuarioRepository, notificationRepository);
    }

    @Bean
    public GetCartSuggestions getCartSuggestions(
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            SugerenciaIARepository sugerenciaIARepository) {
        return new GetCartSuggestions(
                alumnoRepository,
                productRepository,
                purchaseRepository,
                sugerenciaIARepository);
    }

    @Bean
    public ProcessPresentialPurchase processPresentialPurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            PurchaseValidator purchaseValidator,
            InventoryAvailabilityService inventoryAvailabilityService,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                movimientoBilleteraRepository);
    }

    @Bean
    public CreateAssistantAdvancePurchase createAssistantAdvancePurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            RecessTimeSlotResolver recessTimeSlotResolver,
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator,
            WithdrawalCodeService withdrawalCodeService,
            Clock clock,
            InventoryAvailabilityService inventoryAvailabilityService,
            AdvancePurchaseRulesService advancePurchaseRulesService,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                recessTimeSlotResolver,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                clock,
                inventoryAvailabilityService,
                advancePurchaseRulesService,
                movimientoBilleteraRepository);
    }

    @Bean
    public GetPresentialPurchase getPurchase(PurchaseRepository purchaseRepository) {
        return new GetPresentialPurchase(purchaseRepository);
    }

    @Bean
    public GetPurchaseStatus getPurchaseStatus(
            PurchaseRepository purchaseRepository,
            ConsumptionAccessRepository consumptionAccessRepository) {
        return new GetPurchaseStatus(purchaseRepository, consumptionAccessRepository);
    }

    @Bean
    public UpdatePurchaseStatus updatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new UpdatePurchaseStatus(
                purchaseRepository,
                inventoryAvailabilityService,
                walletRepository,
                realtimeEventPublisher,
                movimientoBilleteraRepository);
    }

    @Bean
    public CancelPresentialPurchase cancelPurchase(PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository franjaHorariaRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                franjaHorariaRepository,
                movimientoBilleteraRepository);
    }

    @Bean
    public ManageRestrictions administrarRestricciones(
            AlumnoRepository alumnoRepository,
            ClasificacionSaludRepository clasificacionRepository) {
        return new ManageRestrictions(alumnoRepository, clasificacionRepository);
    }

    @Bean
    public DetectStudentPreferences detectStudentPreferences(
            AlumnoRepository alumnoRepository,
            PreferenceAIRepository preferenceAIRepository,
            SugerenciaIARepository sugerenciaIARepository,
            PurchaseRepository purchaseRepository,
            InventoryRepository inventoryRepository) {
        return new DetectStudentPreferences(alumnoRepository, preferenceAIRepository,
                sugerenciaIARepository, purchaseRepository, inventoryRepository);
    }

    @Bean
    public GetHealthRestrictions obtenerRestriccionesSalud(
            AlumnoRepository alumnoRepository) {
        return new GetHealthRestrictions(alumnoRepository);
    }

    @Bean
    public AddHealthRestriction agregarRestriccionSalud(
            AlumnoRepository alumnoRepository,
            ClasificacionSaludRepository clasificacionRepository) {
        return new AddHealthRestriction(alumnoRepository, clasificacionRepository);
    }

    @Bean
    public RemoveHealthRestrictions eliminarRestriccionSalud(
            AlumnoRepository alumnoRepository) {
        return new RemoveHealthRestrictions(alumnoRepository);
    }

    @Bean
    public GetHealthRestrictionsCatalog obtenerCatalogoRestriccionesSalud(
            ClasificacionSaludRepository clasificacionSaludRepository) {
        return new GetHealthRestrictionsCatalog(clasificacionSaludRepository);
    }

    @Bean
    public GetTimeSlotsBySchool getTimeSlotsBySchool(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new GetTimeSlotsBySchool(franjaHorariaRepository);
    }

    @Bean
    public GetActiveSchools getActiveSchools(ColegioRepository colegioRepository) {
        return new GetActiveSchools(colegioRepository);
    }

    @Bean
    public GetGradesBySchool getGradesBySchool(
            ColegioRepository colegioRepository,
            GradoRepository gradoRepository) {
        return new GetGradesBySchool(colegioRepository, gradoRepository);
    }

    @Bean
    public CreateTimeRestriction createTimeRestriction(
            RestriccionHorariaRepository restriccionHorariaRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository) {
        return new CreateTimeRestriction(
                restriccionHorariaRepository,
                usuarioRepository,
                alumnoRepository);
    }

    @Bean
    public DisableTimeRestriction disableTimeRestriction(
            RestriccionHorariaRepository restriccionHorariaRepository) {
        return new DisableTimeRestriction(restriccionHorariaRepository);
    }

    @Bean
    public ObtenerHistorialAlumno obtenerHistorialAlumno(PurchaseRepository purchaseRepository) {
        return new ObtenerHistorialAlumno(purchaseRepository);
    }

    @Bean
    public ObtenerHistorialPendienteAlumno obtenerHistorialPendienteAlumno(
            PurchaseRepository purchaseRepository) {
        return new ObtenerHistorialPendienteAlumno(purchaseRepository);
    }

    @Bean
    public ObtenerHistorialTutor obtenerHistorialTutor(PurchaseRepository purchaseRepository,
            TutorRepository tutorRepository) {
        return new ObtenerHistorialTutor(purchaseRepository, tutorRepository);
    }

    @Bean
    public GenerarMenuBuffet generarMenuBuffet(AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PresupuestoRepository presupuestoRepository,
            PurchaseRepository purchaseRepository,
            CalculadorPeriodoPresupuesto calculadorPeriodo) {
        return new GenerarMenuBuffet(alumnoRepository,
                productRepository,
                presupuestoRepository,
                purchaseRepository,
                calculadorPeriodo);
    }

    @Bean
    public GetTimeRestrictionsByStudent getTimeRestrictionsByStudent(
            RestriccionHorariaRepository restriccionHorariaRepository) {
        return new GetTimeRestrictionsByStudent(restriccionHorariaRepository);
    }

    @Bean
    public UpdateTimeRestriction updateTimeRestriction(
            RestriccionHorariaRepository restriccionHorariaRepository,
            AlumnoRepository alumnoRepository) {
        return new UpdateTimeRestriction(restriccionHorariaRepository, alumnoRepository);
    }

    @Bean
    public GetStudentPreferences obtenerPreferenciasAlumno(
            SugerenciaIARepository sugerenciaIARepository,
            AlumnoRepository alumnoRepository,
            TutorRepository tutorRepository) {
        return new GetStudentPreferences(
                sugerenciaIARepository,
                alumnoRepository,
                tutorRepository);
    }

    @Bean
    public RespondToAISuggestion responderSugerenciaIA(
            SugerenciaIARepository sugerenciaIARepository) {
        return new RespondToAISuggestion(sugerenciaIARepository);
    }

    @Bean
    public PredictStudentSpending predictStudentSpending(
            AlumnoRepository alumnoRepository,
            SpendingPreferenceRepository spendingPreferenceRepository,
            PurchaseSpendingRepository purchaseSpendingRepository,
            SpendingPredictionAnalyzer spendingPredictionAnalyzer,
            AIEventRepository aiEventRepository,
            SpendingPredictionRangeService spendingPredictionRangeService,
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock) {
        return new PredictStudentSpending(
                alumnoRepository,
                spendingPreferenceRepository,
                purchaseSpendingRepository,
                spendingPredictionAnalyzer,
                aiEventRepository,
                spendingPredictionRangeService,
                spendingPredictionCalculator,
                clock);
    }

    @Bean
    public GenerateStatisticalConsumptionAlerts generateStatisticalConsumptionAlerts(
            ConsumptionAlertStudentRepository studentRepository,
            ConsumptionAlertPurchaseRepository purchaseRepository,
            ConsumptionAlertNotificationRepository notificationRepository,
            ConsumptionAlertAnalyzer analyzer,
            StatisticalConsumptionAlertProperties properties,
            Clock clock) {
        return new GenerateStatisticalConsumptionAlerts(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties,
                clock);
    }

    @Bean
    public GenerateHealthyProductRecommendations generateHealthyProductRecommendations(
            ConsumptionAlertStudentRepository studentRepository,
            HealthyRecommendationPurchaseRepository purchaseRepository,
            HealthyRecommendationNotificationRepository notificationRepository,
            HealthyRecommendationAnalyzer analyzer,
            HealthyRecommendationProperties properties,
            Clock clock) {
        return new GenerateHealthyProductRecommendations(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties,
                clock);
    }

    @Bean
    public GetActiveBudgetByStudent getActiveBudgetByStudent(
            PresupuestoRepository presupuestoRepository) {
        return new GetActiveBudgetByStudent(presupuestoRepository);
    }

    @Bean
    public CreateBudget createBudget(
            PresupuestoRepository presupuestoRepository) {
        return new CreateBudget(presupuestoRepository);
    }

    @Bean
    public UpdateBudget updateBudget(
            PresupuestoRepository presupuestoRepository) {
        return new UpdateBudget(presupuestoRepository);
    }

    @Bean
    public DisableBudget disableBudget(
            PresupuestoRepository presupuestoRepository) {
        return new DisableBudget(presupuestoRepository);
    }

    @Bean
    public CheckBudgetDates checkBudgetDates(
            ProductRepository productRepository,
            ValidadorPresupuestoService validadorPresupuestoService) {
        return new CheckBudgetDates(productRepository, validadorPresupuestoService);
    }

    @Bean
    public LoadStockIA loadStockIA(LoadStockIAService loadStockIAService,
                                   CloudinaryService cloudinaryService) {
        return new LoadStockIA(loadStockIAService, cloudinaryService);
    }

    @Bean
    public SaveProduct saveProduct(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository) {
        return new SaveProduct(productRepository, inventoryRepository, categoryRepository);
    }

    @Bean
    public SaveBulkProducts saveBulkProducts(SaveProduct saveProduct) {
        return new SaveBulkProducts(saveProduct);
    }

    @Bean
    public InventoryAvailabilityService inventoryAvailabilityService(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher) {
        return new InventoryAvailabilityService(
                inventoryRepository,
                stockMovementRepository,
                realtimeEventPublisher);
    }

    @Bean
    public AdjustInventoryStock adjustInventoryStock(
            InventoryAvailabilityService inventoryAvailabilityService) {
        return new AdjustInventoryStock(inventoryAvailabilityService);
    }

    @Bean
    public ListInventoryProducts listInventoryProducts(ProductRepository productRepository) {
        return new ListInventoryProducts(productRepository);
    }

    @Bean
    public ListKiosqueroOrders listKiosqueroOrders(PurchaseRepository purchaseRepository) {
        return new ListKiosqueroOrders(purchaseRepository);
    }

    @Bean
    public ListKiosqueroOperationalAlerts listKiosqueroOperationalAlerts(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            Clock clock) {
        return new ListKiosqueroOperationalAlerts(
                purchaseRepository,
                productRepository,
                clock);
    }

    @Bean
    public GetBuffetTimeSlots getBuffetTimeSlots(
            BuffetRepository buffetRepository,
            FranjaHorariaRepository franjaHorariaRepository) {
        return new GetBuffetTimeSlots(buffetRepository, franjaHorariaRepository);
    }

    @Bean
    public GetStockMovements getStockMovements(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository) {
        return new GetStockMovements(inventoryRepository, stockMovementRepository);
    }

    @Bean
    public ExpireAdvancePurchases expireAdvancePurchases(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService) {
        return new ExpireAdvancePurchases(purchaseRepository, inventoryAvailabilityService);
    }

    @Bean
    public GenerateDailyBuffetReport generateDailyBuffetReport(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            DailyBuffetCloseRepository dailyBuffetCloseRepository) {
        return new GenerateDailyBuffetReport(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                dailyBuffetCloseRepository);
    }

    @Bean
    public GetKiosqueroDashboard getKiosqueroDashboard(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            Clock clock) {
        return new GetKiosqueroDashboard(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                clock);
    }

    @Bean
    public CloseDailyBuffet closeDailyBuffet(
            ExpireAdvancePurchases expireAdvancePurchases,
            GenerateDailyBuffetReport generateDailyBuffetReport,
            DailyBuffetCloseRepository dailyBuffetCloseRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher) {
        return new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                dailyBuffetCloseRepository,
                realtimeEventPublisher);
    }

    @Bean
    public GetDailyBuffetCloseStatus getDailyBuffetCloseStatus(
            DailyBuffetCloseRepository dailyBuffetCloseRepository) {
        return new GetDailyBuffetCloseStatus(dailyBuffetCloseRepository);
    }

    @Bean
    public ListDailyBuffetCloses listDailyBuffetCloses(
            DailyBuffetCloseRepository dailyBuffetCloseRepository) {
        return new ListDailyBuffetCloses(dailyBuffetCloseRepository);
    }

    @Bean
    public UpdatePickupSlotRules updatePickupSlotRules(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new UpdatePickupSlotRules(franjaHorariaRepository);
    }

    @Bean
    public GetAllCategories getAllCategories(CategoryRepository categoryRepository) {
        return new GetAllCategories(categoryRepository);
    }

    @Bean
    public GetAllProducts getAllProducts(ProductRepository productRepository) {
        return new GetAllProducts(productRepository);
    }

    @Bean
    public GetProductById getProductById(ProductRepository productRepository) {
        return new GetProductById(productRepository);
    }

    @Bean
    public UpdateProduct updateProduct(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository) {
        return new UpdateProduct(productRepository, inventoryRepository, categoryRepository);
    }

    @Bean
    public DeleteProduct deleteProduct(ProductRepository productRepository) {
        return new DeleteProduct(productRepository);
    }

    @Bean
    public GenerateProductChangeRecommendation generateProductChangeRecommendation(
            ProductRepository productRepository, ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository, KiosqueroRepository kiosqueroRepository,
            GetProductStatistics getProductStatistics) {
        return new GenerateProductChangeRecommendation(
                productRepository, productChangeAnalyzer, aiEventRepository, kiosqueroRepository,
                getProductStatistics);
    }

    @Bean
    public GenerateSeasonalRecommendation generateSeasonalRecommendation(
            GeminiSeasonalSuggester seasonalSuggester,
            OpenMeteoWeatherClient weatherClient,
            ProductRepository productRepository,
            CreatePromotion createPromotion){
        return new GenerateSeasonalRecommendation(
                weatherClient,
                seasonalSuggester,
                productRepository,
                createPromotion);
    }

    @Bean
    public AddProductRecommendation addProductRecommendation(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository,
            InventoryRepository inventoryRepository,
            AIEventRepository aiEventRepository,
            ProductAdditionNotificationRepository notificationRepository,
            ProductStatisticsRepository productStatisticsRepository,
            SugerenciaIARepository sugerenciaIARepository) {
        return new AddProductRecommendation(
                productRepository,
                kiosqueroRepository,
                inventoryRepository,
                aiEventRepository,
                notificationRepository,
                productStatisticsRepository,
                sugerenciaIARepository);
    }

    @Bean
    public GeneratePurchaseRecommendation generatePurchaseRecommendation(
            AlumnoRepository alumnoRepository, PurchaseRepository purchaseRepository,
            PurchaseAnalyzer purchaseAnalyzer, AIEventRepository aiEventRepository,
            PurchaseRecommendationNotificationRepository notificationRepository,
            ConsumptionAlertStudentRepository studentRepository,
            SuggestionAiRepository suggestionAiRepository) {
        return new GeneratePurchaseRecommendation(alumnoRepository,
                purchaseRepository, purchaseAnalyzer, aiEventRepository, notificationRepository,
                studentRepository, suggestionAiRepository);
    }

    @Bean
    public LoadBalanceToChild loadBalanceToChild(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new LoadBalanceToChild(
                usuarioRepository,
                tutorRepository,
                walletRepository,
                movimientoBilleteraRepository);
    }

    @Bean
    public TransferBalanceBetweenChildren transferBalanceBetweenChildren(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            SchoolCreditRepository schoolCreditRepository) {
        return new TransferBalanceBetweenChildren(
                usuarioRepository,
                tutorRepository,
                walletRepository,
                movimientoBilleteraRepository,
                schoolCreditRepository
        );
    }

    @Bean
    public GenerateTopUpLink generateTopUpLink(
            AlumnoRepository alumnoRepository,
            ExternalPaymentService externalPaymentService) {
        return new GenerateTopUpLink(alumnoRepository, externalPaymentService);
    }

    @Bean
    public ConfirmPaymentAndCredit confirmPaymentAndCredit(
            WalletRepository walletRepository,
            ExternalPaymentService externalPaymentService,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new ConfirmPaymentAndCredit(
                walletRepository,
                externalPaymentService,
                movimientoBilleteraRepository);
    }

    @Bean
    public GetProductsByBuffetUser getProductsByBuffetUser(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository) {
        return new GetProductsByBuffetUser(
                productRepository, kiosqueroRepository);
    }

    @Bean
    public GetScheduledPickups getScheduledPickups(PurchaseRepository purchaseRepository) {
        return new GetScheduledPickups(purchaseRepository);
    }

    @Bean
    public GetBuffetByAlumno getBuffetByAlumno(
            BuffetRepository buffetRepository,
            AlumnoRepository alumnoRepository) {
        return new GetBuffetByAlumno(buffetRepository, alumnoRepository);
    }

    @Bean
    public PurchaseSuggestIA purchaseSuggestIA(
            UsuarioService usuarioService,
            SugerenciaIARepository sugerenciaIARepository,
            FranjaHorariaRepository franjaHorariaRepository,
            Clock clock) {
        return new PurchaseSuggestIA(usuarioService, sugerenciaIARepository,
                franjaHorariaRepository, clock);
    }

    @Bean
    public NotifyPurchaseSuggestions notifyPurchaseSuggestions(
            PurchaseSuggestIA purchaseSuggestIA,
            AlumnoRepository alumnoRepository,
            PushNotificationSender pushNotificationSender,
            UsuarioService usuarioService,
            ObjectMapper objectMapper) {
        return new NotifyPurchaseSuggestions(purchaseSuggestIA, alumnoRepository,
                pushNotificationSender, usuarioService, objectMapper);
    }

    @Bean
    public GenerateLowBalanceAlerts generateLowBalanceAlerts(
            WalletRepository walletRepository,
            AlumnoRepository alumnoRepository,
            TutorRepository tutorRepository,
            PushNotificationSender pushNotificationSender) {
        return new GenerateLowBalanceAlerts(
                walletRepository,
                alumnoRepository,
                tutorRepository,
                pushNotificationSender);
    }

    @Bean
    public PrizeHealthyPurchase prizeHealthyPurchase(
            PurchaseRepository purchaseRepository,
            AlumnoRepository alumnoRepository) {
        return new PrizeHealthyPurchase(purchaseRepository, alumnoRepository);
    }

    @Bean
    public GetStudentRewardStatus getStudentRewardStatus(AlumnoRepository alumnoRepository) {
        return new GetStudentRewardStatus(alumnoRepository);
    }

    @Bean
    public CreatePromotion createPromotion(PromotionRepository promotionRepository) {
        return new CreatePromotion(promotionRepository);
    }

    @Bean
    public UpdatePromotion updatePromotion(PromotionRepository promotionRepository) {
        return new UpdatePromotion(promotionRepository);
    }

    @Bean
    public DeletePromotion deletePromotion(PromotionRepository promotionRepository) {
        return new DeletePromotion(promotionRepository);
    }

    @Bean
    public GetActivePromotions getActivePromotions(PromotionRepository promotionRepository) {
        return new GetActivePromotions(promotionRepository);
    }

    @Bean
    public GetActivePromotionsByBuffet getActivePromotionsByBuffet(
            PromotionRepository promotionRepository) {
        return new GetActivePromotionsByBuffet(promotionRepository);
    }

    @Bean
    public GetPromotionById getPromotionById(PromotionRepository promotionRepository) {
        return new GetPromotionById(promotionRepository);
    }

    @Bean
    public SaveCarritoFavorito saveCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository) {
        return new SaveCarritoFavorito(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                productRepository);
    }

    @Bean
    public GetCarritosFavoritosByTutor getCarritosFavoritosByTutor(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        return new GetCarritosFavoritosByTutor(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository);
    }

    @Bean
    public DeleteCarritoFavorito deleteCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        return new DeleteCarritoFavorito(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository);
    }
    @Bean
    public GeminiBulkStockAnalyzer bulkStockAnalyzer(GeminiTextClient geminiTextClient,
                                                     ObjectMapper objectMapper) {
        return new GeminiBulkStockAnalyzer(geminiTextClient, objectMapper);
    }

    @Bean
    public ParseBulkStock parseBulkStock(BulkStockAnalyzer bulkStockAnalyzer,
                                         CategoryRepository categoryRepository,
                                         ClasificacionSaludRepository saludRepository,
                                         ClasificacionTipoRepository tipoRepository) {
        return new ParseBulkStock(bulkStockAnalyzer, categoryRepository,
                saludRepository, tipoRepository);
    }

    @Bean
    public ManagePromotionStatus managePromotionStatus(PromotionRepository promotionRepository) {
        return new ManagePromotionStatus(promotionRepository);
    }

    @Bean
    public SetNotificationAsAlreadyRead setNotificationAsAlreadyRead(
            NotificationRepository notificationRepository) {
        return new SetNotificationAsAlreadyRead(notificationRepository);
    }

    @Bean
    public GetTutorGlobalDashboard getTutorGlobalDashboard(
            ObtenerHijosPorPadre obtenerHijosPorPadre,
            GetActiveBudgetByStudent getActiveBudgetByStudent,
            GetStudentRewardStatus getStudentRewardStatus,
            PredictStudentSpending predictStudentSpending,
            ObtenerHistorialAlumno obtenerHistorialAlumno,
            TutorRepository tutorRepository) {
        return new GetTutorGlobalDashboard(
                obtenerHijosPorPadre,
                getActiveBudgetByStudent,
                getStudentRewardStatus,
                predictStudentSpending,
                obtenerHistorialAlumno,
                tutorRepository
        );
    }

    @Bean
    public UploadProductImage uploadProductImage(
            com.recred.backend.infrastructure.cloudinary.CloudinaryService cloudinaryService,
            InventoryRepository inventoryRepository) {
        return new UploadProductImage(cloudinaryService, inventoryRepository);
    }

    @Bean
    public UpdateTutorDashboardConfig updateTutorDashboardConfig(TutorRepository tutorRepository) {
        return new UpdateTutorDashboardConfig(tutorRepository);
    }

    @Bean
    public EnableSchoolCredit enableSchoolCredit(
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            SchoolCreditRepository schoolCreditRepository,
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            Clock clock) {
        return new EnableSchoolCredit(
                tutorRepository, alumnoRepository, schoolCreditRepository,
                purchaseRepository, walletRepository, clock);
    }

    @Bean
    public GetLastRecharge getLastRecharge(
            AlumnoRepository alumnoRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new GetLastRecharge(alumnoRepository, movimientoBilleteraRepository);
    }

    @Bean
    public GetActiveSchoolCredit getActiveSchoolCredit(
            SchoolCreditRepository schoolCreditRepository) {
        return new GetActiveSchoolCredit(schoolCreditRepository);
    }

    @Bean
    public GetSuggestionByNotificationId getSuggestionByNotificationId(
            SuggestionAiRepository suggestionAiRepository) {
        return new GetSuggestionByNotificationId(suggestionAiRepository);
    }

    @Bean
    public GetSuggestionByType getSuggestionByType(
            SuggestionAiRepository suggestionAiRepository,
            KiosqueroRepository kiosqueroRepository) {
        return new GetSuggestionByType(suggestionAiRepository, kiosqueroRepository);
    }
}
