package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.ActivateSchoolSubscription;
import com.recred.backend.application.AddFavoriteProduct;
import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.ApplyInventoryQuickAction;
import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.PaySchoolCredit;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetSchoolCreditHistory;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import com.recred.backend.domain.services.StorageService;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;
import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.ListSuppliers;
import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.ConfirmSchoolSubscriptionPayment;
import com.recred.backend.application.ConfirmUserSubscriptionPayment;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateGrade;
import com.recred.backend.application.CreatePromotion;
import com.recred.backend.application.CreateSchoolSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DeactivateGrade;
import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.DeleteFavoriteProduct;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeletePromotion;
import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.ExpireUserSubscriptions;
import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.FindPurchaseSuggestionForToday;
import com.recred.backend.application.FindPurchaseSuggestionForTomorrow;
import com.recred.backend.application.GenerateBuffetMenu;
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
import com.recred.backend.application.GenerateSummaryNotification;
import com.recred.backend.application.GenerateUserSubscriptionExpirationWarnings;
import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetActivePromotions;
import com.recred.backend.application.GetActivePromotionsByBuffet;
import com.recred.backend.application.GetActiveLevels;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetChildrenByTutor;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.application.GetGrade;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.GetKiosqueroPayoutConfig;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.application.GetLastSummary;
import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetPendingHistoryByStudent;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetPromotionById;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetSchoolOverview;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.application.GetTutorSchools;
import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ListSchoolGrades;
import com.recred.backend.application.ListSuppliers;
import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.GetAllPromotionsByBuffet;
import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.ManagePromotionStatus;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PrizeHealthyPurchase;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.ProcessScheduledPayouts;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.TogglePromotionStatus;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.ReactivateGrade;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.RegisterBuffet;
import com.recred.backend.application.RegisterSeller;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.GetSchoolOverview;

import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveFavoriteCart;
import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SchoolAssistantAnswerFormatter;
import com.recred.backend.application.SchoolAssistantContentFormatter;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.application.SchoolAssistantConversationService;
import com.recred.backend.application.SchoolAssistantContextProvider;
import com.recred.backend.application.SchoolAssistantContextResolver;
import com.recred.backend.application.SchoolAssistantFixedTopicResponses;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.application.SchoolAssistantIntentDetector;
import com.recred.backend.application.SchoolAssistantMessageClassifier;
import com.recred.backend.application.SchoolAssistantPendingPurchaseParser;
import com.recred.backend.application.SchoolAssistantPendingPurchaseResponder;
import com.recred.backend.application.SchoolAssistantPurchaseCancellationHandler;
import com.recred.backend.application.SchoolAssistantPurchaseCancellationResponder;
import com.recred.backend.application.SchoolAssistantPurchaseFailureMessages;
import com.recred.backend.application.SchoolAssistantPurchasePatternService;
import com.recred.backend.application.SchoolAssistantPurchaseRequestParser;
import com.recred.backend.application.SchoolAssistantPurchaseResponder;
import com.recred.backend.application.SchoolAssistantQuickPurchaseHandler;
import com.recred.backend.application.SchoolAssistantQuickPurchaseReplyFactory;
import com.recred.backend.application.SchoolAssistantQuestionResponder;
import com.recred.backend.application.SchoolAssistantReplyBuilder;
import com.recred.backend.application.SchoolAssistantReplyFormatter;
import com.recred.backend.application.SchoolAssistantRoleCatalog;
import com.recred.backend.application.SchoolAssistantSuggestionFactory;
import com.recred.backend.application.SchoolAssistantTutorFactsFormatter;
import com.recred.backend.application.SchoolSubscriptionPriceCatalog;
import com.recred.backend.application.SetNotificationAsAlreadyRead;
import com.recred.backend.application.StudentAssistantContextBuilder;
import com.recred.backend.application.StudentAssistantContextProvider;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.TutorAssistantContextBuilder;
import com.recred.backend.application.TutorAssistantContextProvider;
import com.recred.backend.application.TutorChildrenPlanPropagator;
import com.recred.backend.application.KiosqueroAssistantContextBuilder;
import com.recred.backend.application.KiosqueroAssistantContextProvider;
import com.recred.backend.application.SubscriptionPriceCatalog;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateGrade;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdatePromotion;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadSupplierPriceList;
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
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.NivelRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.PagoSuscripcionColegioRepository;
import com.recred.backend.domain.repositories.PagoSuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProductStatisticsRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import com.recred.backend.domain.services.ExternalPaymentService;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.NoOpTutorInvitationEmailSender;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import com.recred.backend.domain.services.PayoutPaymentService;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.PurchaseValidatorImpl;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.StorageService;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;
import com.recred.backend.domain.services.TutorInvitationEmailSender;
import com.recred.backend.domain.services.TutorService;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.domain.services.WithdrawalCodeServiceImpl;
import com.recred.backend.infrastructure.email.SmtpMailProperties;
import com.recred.backend.infrastructure.email.SmtpTutorInvitationEmailSender;
import com.recred.backend.infrastructure.gemini.GeminiBulkStockAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiParentSummaryAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.gemini.GeminiSupplierPriceListAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.openMeteo.NominatimGeocodingClient;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class UseCases {

    @Bean
    public GenerateComboSuggestion generateComboSuggestion(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository,
            UsuarioRepository usuarioRepository) {
        return new GenerateComboSuggestion(
                productRepository, kiosqueroRepository, usuarioRepository);
    }

    @Bean
    public RegisterBuffet registerBuffet(
            ColegioRepository colegioRepository,
            BuffetRepository buffetRepository) {
        return new RegisterBuffet(colegioRepository, buffetRepository);
    }

    @Bean
    public GetSchoolOverview getSchoolOverview(
            UsuarioRepository usuarioRepository,
            ColegioRepository colegioRepository,
            BuffetRepository buffetRepository,
            SuscripcionColegioRepository suscripcionColegioRepository,
            Clock clock) {
        return new GetSchoolOverview(
                usuarioRepository,
                colegioRepository,
                buffetRepository,
                suscripcionColegioRepository,
                clock);
    }

    @Bean
    public RegisterSeller registerSeller(
            BuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository,
            KiosqueroRepository kiosqueroRepository,
            com.recred.backend.domain.services.CognitoSellerIdentityService cognito) {
        return new RegisterSeller(buffetRepository, usuarioRepository,
                kiosqueroRepository, cognito);
    }


    @Bean
    public ApplyInventoryQuickAction applyInventoryQuickAction(
            InventoryAvailabilityService inventoryAvailabilityService) {
        return new ApplyInventoryQuickAction(inventoryAvailabilityService);
    }

    @Bean
    public SubmitSchoolRegistration submitSchoolRegistration(
            SchoolRegistrationRequestRepository repository,
            ColegioRepository colegioRepository,
            UsuarioRepository usuarioRepository) {
        return new SubmitSchoolRegistration(repository, colegioRepository, usuarioRepository);
    }

    @Bean
    public ApproveSchoolRegistration approveSchoolRegistration(
            SchoolRegistrationRequestRepository registrationRepository,
            ColegioRepository colegioRepository,
            UsuarioRepository usuarioRepository,
            CognitoDirectorIdentityService cognitoService) {
        return new ApproveSchoolRegistration(
                registrationRepository, colegioRepository, usuarioRepository, cognitoService);
    }

    @Bean
    public RejectSchoolRegistration rejectSchoolRegistration(
            SchoolRegistrationRequestRepository repository) {
        return new RejectSchoolRegistration(repository);
    }

    @Bean
    public com.recred.backend.application.GetSchoolRegistrations getSchoolRegistrations(
            SchoolRegistrationRequestRepository repository) {
        return new com.recred.backend.application.GetSchoolRegistrations(repository);
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
    public GetLastSummary getLastSummary(SummaryRepository summaryRepository) {
        return new GetLastSummary(summaryRepository);
    }

    @Bean
    public GenerateSummaryNotification generateSummaryNotification(
            NotificationRepository notificationRepository) {
        return new GenerateSummaryNotification(notificationRepository);
    }

    @Bean
    public ParentSummaryAnalyzer parentSummaryAnalyzer(GeminiTextClient geminiTextClient) {
        return new GeminiParentSummaryAnalyzer(geminiTextClient);
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
    public WithdrawalCodeService withdrawalCodeService(PurchaseRepository purchaseRepository) {
        return new WithdrawalCodeServiceImpl(purchaseRepository);
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
            BuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            PromotionRepository promotionRepository,
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
                promotionRepository,
                movimientoBilleteraRepository);
    }

    @Bean
    public RecessTimeSlotResolver recessTimeSlotResolver(
            FranjaHorariaRepository franjaHorariaRepository,
            BuffetRepository buffetRepository) {
        return new RecessTimeSlotResolver(franjaHorariaRepository, buffetRepository);
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
    public ManualProductLock bloquearProductoManual(AlumnoRepository alumnoRepository,
                                                    ProductRepository productRepository) {
        return new ManualProductLock(alumnoRepository, productRepository);
    }

    @Bean
    public ManualProductUnlock desbloquearProductoManual(AlumnoRepository alumnoRepository) {
        return new ManualProductUnlock(alumnoRepository);
    }

    @Bean
    public AddFavoriteProduct agregarProductoFavorito(AlumnoRepository alumnoRepository,
                                                      ProductRepository productRepository,
                                                      TutorRepository tutorRepository) {
        return new AddFavoriteProduct(alumnoRepository, productRepository, tutorRepository);
    }

    @Bean
    public DeleteFavoriteProduct removerProductoFavorito(AlumnoRepository alumnoRepository) {
        return new DeleteFavoriteProduct(alumnoRepository);
    }

    @Bean
    public GetFavoritesByStudent getFavoritosByAlumno(AlumnoRepository alumnoRepository) {
        return new GetFavoritesByStudent(alumnoRepository);
    }

    @Bean
    public UpdateBudgetRules updateBudgetRules(PresupuestoRepository repository) {
        return new UpdateBudgetRules(repository);
    }

    @Bean
    public GetChildrenByTutor obtenerHijosPorPadre(TutorRepository tutorRepository) {
        return new GetChildrenByTutor(tutorRepository);
    }

    @Bean
    public CreateStudentFromParent createStudentFromParent(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            GradoRepository gradoRepository,
            TutorSchoolRepository tutorSchoolRepository,
            CognitoStudentIdentityService cognitoStudentIdentityService) {
        return new CreateStudentFromParent(
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                gradoRepository,
                tutorSchoolRepository,
                cognitoStudentIdentityService);
    }

    @Bean
    public TutorInvitationEmailSender tutorInvitationEmailSender(
            @Value("${recred.mail.host:}") String host,
            @Value("${recred.mail.port:587}") int port,
            @Value("${recred.mail.username:}") String username,
            @Value("${recred.mail.password:}") String password,
            @Value("${recred.mail.from:}") String from,
            @Value("${recred.mail.smtp-auth:true}") boolean auth,
            @Value("${recred.mail.smtp-starttls-enable:true}") boolean startTls,
            @Value("${recred.mail.ssl-trust:}") String sslTrust,
            @Value("${recred.mail.connection-timeout:5000}") int connectionTimeout,
            @Value("${recred.mail.timeout:3000}") int timeout,
            @Value("${recred.mail.write-timeout:5000}") int writeTimeout) {
        SmtpMailProperties properties = new SmtpMailProperties(
                host,
                port,
                username,
                password,
                from,
                auth,
                startTls,
                sslTrust,
                connectionTimeout,
                timeout,
                writeTimeout);
        if (properties.enabled()) {
            return new SmtpTutorInvitationEmailSender(properties);
        }
        return new NoOpTutorInvitationEmailSender();
    }

    @Bean
    public InviteTutorToSchool inviteTutorToSchool(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            TutorSchoolRepository tutorSchoolRepository,
            TutorInvitationRepository tutorInvitationRepository,
            TutorInvitationEmailSender tutorInvitationEmailSender,
            Clock clock,
            @Value("${recred.frontend.base-url:http://localhost:4200}") String frontendBaseUrl) {
        return new InviteTutorToSchool(
                usuarioRepository,
                tutorRepository,
                tutorSchoolRepository,
                tutorInvitationRepository,
                tutorInvitationEmailSender,
                clock,
                frontendBaseUrl);
    }

    @Bean
    public GetTutorInvitation getTutorInvitation(
            TutorInvitationRepository tutorInvitationRepository,
            Clock clock) {
        return new GetTutorInvitation(tutorInvitationRepository, clock);
    }

    @Bean
    public AcceptTutorInvitation acceptTutorInvitation(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            TutorSchoolRepository tutorSchoolRepository,
            TutorInvitationRepository tutorInvitationRepository,
            Clock clock) {
        return new AcceptTutorInvitation(
                usuarioRepository,
                tutorRepository,
                tutorSchoolRepository,
                tutorInvitationRepository,
                clock);
    }

    @Bean
    public GetTutorSchools getTutorSchools(
            TutorRepository tutorRepository,
            TutorSchoolRepository tutorSchoolRepository) {
        return new GetTutorSchools(tutorRepository, tutorSchoolRepository);
    }

    @Bean
    public ImportTutorInvitationsFromCsv importTutorInvitationsFromCsv(
            InviteTutorToSchool inviteTutorToSchool) {
        return new ImportTutorInvitationsFromCsv(inviteTutorToSchool);
    }

    @Bean
    public UpdateStudentPhoto actualizarFotoAlumno(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository) {
        return new UpdateStudentPhoto(usuarioRepository, tutorRepository, alumnoRepository);
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
                presupuestoRepository, purchaseRepository,calculadorPeriodo);
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
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            ProductRepository productRepository) {
        return new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                movimientoBilleteraRepository,
                productRepository);
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
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            TutorRepository tutorRepository,
            NotificationRepository notificationRepository,
            PushNotificationSender pushNotificationSender) {
        return new UpdatePurchaseStatus(
                purchaseRepository,
                inventoryAvailabilityService,
                walletRepository,
                realtimeEventPublisher,
                movimientoBilleteraRepository,
                tutorRepository,
                notificationRepository,
                pushNotificationSender);
    }

    @Bean
    public CancelPresentialPurchase cancelPurchase(PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository franjaHorariaRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            Clock clock) {
        return new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                franjaHorariaRepository,
                movimientoBilleteraRepository,
                clock);
    }

    @Bean
    public com.recred.backend.application.RevertPurchaseCancellation revertPurchaseCancellation(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new com.recred.backend.application.RevertPurchaseCancellation(
                purchaseRepository,
                walletRepository,
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
    public ListSchoolTimeSlots listSchoolTimeSlots(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new ListSchoolTimeSlots(franjaHorariaRepository);
    }

    @Bean
    public GetTimeSlot getTimeSlot(FranjaHorariaRepository franjaHorariaRepository) {
        return new GetTimeSlot(franjaHorariaRepository);
    }

    @Bean
    public CreateTimeSlot createTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository,
            ColegioRepository colegioRepository) {
        return new CreateTimeSlot(franjaHorariaRepository, colegioRepository);
    }

    @Bean
    public UpdateTimeSlot updateTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository,
            ColegioRepository colegioRepository,
            PurchaseRepository purchaseRepository,
            Clock clock) {
        return new UpdateTimeSlot(
                franjaHorariaRepository,
                colegioRepository,
                purchaseRepository,
                clock);
    }

    @Bean
    public DeactivateTimeSlot deactivateTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new DeactivateTimeSlot(franjaHorariaRepository);
    }

    @Bean
    public ReactivateTimeSlot reactivateTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository) {
        return new ReactivateTimeSlot(franjaHorariaRepository);
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
    public GetActiveLevels getActiveLevels(NivelRepository nivelRepository) {
        return new GetActiveLevels(nivelRepository);
    }

    @Bean
    public ListSchoolGrades listSchoolGrades(
            GradoRepository gradoRepository,
            ColegioRepository colegioRepository) {
        return new ListSchoolGrades(gradoRepository, colegioRepository);
    }

    @Bean
    public GetGrade getGrade(GradoRepository gradoRepository) {
        return new GetGrade(gradoRepository);
    }

    @Bean
    public CreateGrade createGrade(
            GradoRepository gradoRepository,
            ColegioRepository colegioRepository,
            NivelRepository nivelRepository) {
        return new CreateGrade(gradoRepository, colegioRepository, nivelRepository);
    }

    @Bean
    public UpdateGrade updateGrade(
            GradoRepository gradoRepository,
            ColegioRepository colegioRepository,
            NivelRepository nivelRepository) {
        return new UpdateGrade(gradoRepository, colegioRepository, nivelRepository);
    }

    @Bean
    public DeactivateGrade deactivateGrade(GradoRepository gradoRepository) {
        return new DeactivateGrade(gradoRepository);
    }

    @Bean
    public ReactivateGrade reactivateGrade(GradoRepository gradoRepository) {
        return new ReactivateGrade(gradoRepository);
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
    public GetHistoryByStudent obtenerHistorialAlumno(PurchaseRepository purchaseRepository) {
        return new GetHistoryByStudent(purchaseRepository);
    }

    @Bean
    public GetPendingHistoryByStudent obtenerHistorialPendienteAlumno(
            PurchaseRepository purchaseRepository) {
        return new GetPendingHistoryByStudent(purchaseRepository);
    }

    @Bean
    public GetTutorHistory getTutorHistory(PurchaseRepository purchaseRepository,
            TutorRepository tutorRepository) {
        return new GetTutorHistory(purchaseRepository, tutorRepository);
    }

    @Bean
    public GenerateBuffetMenu generateBuffetMenu(AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PresupuestoRepository presupuestoRepository,
            PurchaseRepository purchaseRepository,
            CalculadorPeriodoPresupuesto calculadorPeriodo) {
        return new GenerateBuffetMenu(alumnoRepository,
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
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock) {
        return new PredictStudentSpending(
                alumnoRepository,
                spendingPreferenceRepository,
                purchaseSpendingRepository,
                spendingPredictionAnalyzer,
                aiEventRepository,
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
            PromotionRepository promotionRepository,
            ValidadorPresupuestoService validadorPresupuestoService) {
        return new CheckBudgetDates(
                productRepository,
                promotionRepository,
                validadorPresupuestoService
        );
    }

    @Bean
    public LoadStockIA loadStockIA(LoadStockIAService loadStockIAService,
                                   StorageService storageService) {
        return new LoadStockIA(loadStockIAService, storageService);
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
    public GetProductChangeRecommendation getProductChangeRecommendation(
            KiosqueroRepository kiosqueroRepository, AIEventRepository aiEventRepository,
            UsuarioRepository usuarioRepository) {
        return new GetProductChangeRecommendation(
                kiosqueroRepository, aiEventRepository, usuarioRepository);
    }

    @Bean
    public GenerateSeasonalRecommendation generateSeasonalRecommendation(
            OpenMeteoWeatherClient climaService,
            NominatimGeocodingClient geocodingClient,
            GeminiSeasonalSuggester geminiSeasonalSuggesterService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        return new GenerateSeasonalRecommendation(
                climaService,
                geocodingClient,
                geminiSeasonalSuggesterService,
                productRepository,
                categoryRepository);
    }

    @Bean
    public AddProductRecommendation addProductRecommendation(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository,
            AIEventRepository aiEventRepository,
            ProductAdditionNotificationRepository notificationRepository,
            ProductStatisticsRepository productStatisticsRepository,
            SugerenciaIARepository sugerenciaIARepository) {
        return new AddProductRecommendation(
                productRepository,
                kiosqueroRepository,
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
    public FindPurchaseSuggestionForToday findPurchaseSuggestionForToday(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository,
            Clock clock) {
        return new FindPurchaseSuggestionForToday(
                suggestionAiRepository, usuarioRepository, alumnoRepository, clock);
    }

    @Bean
    public FindPurchaseSuggestionForTomorrow findPurchaseSuggestionForTomorrow(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository,
            Clock clock) {
        return new FindPurchaseSuggestionForTomorrow(
                suggestionAiRepository, usuarioRepository, alumnoRepository, clock);
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
    public SubscriptionPriceCatalog subscriptionPriceCatalog() {
        return new SubscriptionPriceCatalog();
    }

    @Bean
    public CreateUserSubscriptionPayment createUserSubscriptionPayment(
            UsuarioRepository usuarioRepository,
            ExternalPaymentService externalPaymentService,
            SubscriptionPriceCatalog priceCatalog) {
        return new CreateUserSubscriptionPayment(
                usuarioRepository,
                externalPaymentService,
                priceCatalog);
    }

    @Bean
    public ConfirmUserSubscriptionPayment confirmUserSubscriptionPayment(
            PagoSuscripcionUsuarioRepository paymentRepository,
            UsuarioRepository usuarioRepository,
            ActivateUserSubscription activateUserSubscription,
            Clock clock) {
        return new ConfirmUserSubscriptionPayment(
                paymentRepository,
                usuarioRepository,
                activateUserSubscription,
                clock);
    }

    @Bean
    public SchoolSubscriptionPriceCatalog schoolSubscriptionPriceCatalog() {
        return new SchoolSubscriptionPriceCatalog();
    }

    @Bean
    public CreateSchoolSubscriptionPayment createSchoolSubscriptionPayment(
            ColegioRepository colegioRepository,
            ExternalPaymentService externalPaymentService,
            SchoolSubscriptionPriceCatalog priceCatalog) {
        return new CreateSchoolSubscriptionPayment(
                colegioRepository,
                externalPaymentService,
                priceCatalog);
    }

    @Bean
    public ActivateSchoolSubscription activateSchoolSubscription(
            ColegioRepository colegioRepository,
            SuscripcionColegioRepository subscriptionRepository,
            Clock clock) {
        return new ActivateSchoolSubscription(
                colegioRepository,
                subscriptionRepository,
                clock);
    }

    @Bean
    public ConfirmSchoolSubscriptionPayment confirmSchoolSubscriptionPayment(
            PagoSuscripcionColegioRepository paymentRepository,
            ActivateSchoolSubscription activateSchoolSubscription,
            Clock clock) {
        return new ConfirmSchoolSubscriptionPayment(
                paymentRepository,
                activateSchoolSubscription,
                clock);
    }

    @Bean
    public GenerateUserSubscriptionExpirationWarnings
            generateUserSubscriptionExpirationWarnings(
                    SuscripcionUsuarioRepository subscriptionRepository,
                    NotificationRepository notificationRepository,
                    Clock clock) {
        return new GenerateUserSubscriptionExpirationWarnings(
                subscriptionRepository,
                notificationRepository,
                clock);
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
    public GetBuffetByStudent getBuffetByAlumno(
            BuffetRepository buffetRepository,
            AlumnoRepository alumnoRepository) {
        return new GetBuffetByStudent(buffetRepository, alumnoRepository);
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
            PushNotificationSender pushNotificationSender,
            UsuarioService usuarioService,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository
    ) {
        return new NotifyPurchaseSuggestions(
                purchaseSuggestIA, pushNotificationSender,
                usuarioService, objectMapper, notificationRepository);
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
    public CreatePromotion createPromotion(PromotionRepository promotionRepository, Clock clock) {
        return new CreatePromotion(promotionRepository, clock);
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
    public GetAllPromotionsByBuffet getAllPromotionsByBuffet(
            PromotionRepository promotionRepository) {
        return new GetAllPromotionsByBuffet(promotionRepository);
    }

    @Bean
    public TogglePromotionStatus togglePromotionStatus(
            PromotionRepository promotionRepository) {
        return new TogglePromotionStatus(promotionRepository);
    }

    @Bean
    public GetPromotionById getPromotionById(PromotionRepository promotionRepository) {
        return new GetPromotionById(promotionRepository);
    }

    @Bean
    public SaveFavoriteCart saveCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository) {
        return new SaveFavoriteCart(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                productRepository);
    }

    @Bean
    public GetFavoriteCartsByTutor getCarritosFavoritosByTutor(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        return new GetFavoriteCartsByTutor(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository);
    }

    @Bean
    public DeleteFavoriteCart deleteCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        return new DeleteFavoriteCart(
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
            GetActiveBudgetByStudent getActiveBudgetByStudent,
            GetStudentRewardStatus getStudentRewardStatus,
            PredictStudentSpending predictStudentSpending,
            GetHistoryByStudent getHistoryByStudent,
            TutorRepository tutorRepository) {
        return new GetTutorGlobalDashboard(
                getActiveBudgetByStudent,
                getStudentRewardStatus,
                predictStudentSpending,
                getHistoryByStudent,
                tutorRepository
        );
    }

    @Bean
    public UploadProductImage uploadProductImage(
            StorageService storageService,
            InventoryRepository inventoryRepository) {
        return new UploadProductImage(storageService, inventoryRepository);
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
                walletRepository, clock);
    }

    @Bean
    public TutorChildrenPlanPropagator tutorChildrenPlanPropagator(
            TutorRepository tutorRepository,
            UsuarioRepository usuarioRepository) {
        return new TutorChildrenPlanPropagator(tutorRepository, usuarioRepository);
    }

    @Bean
    public ActivateUserSubscription activateUserSubscription(
            UsuarioRepository usuarioRepository,
            SuscripcionUsuarioRepository subscriptionRepository,
            TutorChildrenPlanPropagator tutorChildrenPlanPropagator,
            Clock clock) {
        return new ActivateUserSubscription(
                usuarioRepository,
                subscriptionRepository,
                tutorChildrenPlanPropagator,
                clock);
    }

    @Bean
    public ExpireUserSubscriptions expireUserSubscriptions(
            SuscripcionUsuarioRepository subscriptionRepository,
            UsuarioRepository usuarioRepository,
            TutorChildrenPlanPropagator tutorChildrenPlanPropagator,
            Clock clock) {
        return new ExpireUserSubscriptions(
                subscriptionRepository,
                usuarioRepository,
                tutorChildrenPlanPropagator,
                clock);
    }

    @Bean
    public PaySchoolCredit paySchoolCredit(
            SchoolCreditRepository schoolCreditRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new PaySchoolCredit(
                schoolCreditRepository,
                walletRepository,
                movimientoBilleteraRepository
        );
    }

    @Bean
    public GetLastRecharge getLastRecharge(
            AlumnoRepository alumnoRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        return new GetLastRecharge(alumnoRepository, movimientoBilleteraRepository);
    }

    @Bean
    public GetSchoolCreditHistory getSchoolCreditHistory(
            SchoolCreditRepository schoolCreditRepository) {
        return new GetSchoolCreditHistory(schoolCreditRepository);
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
            KiosqueroRepository kiosqueroRepository,
            UsuarioRepository usuarioRepository) {
        return new GetSuggestionByType(
                suggestionAiRepository, kiosqueroRepository, usuarioRepository);
    }

    @Bean
    public GeminiSupplierPriceListAnalyzer supplierPriceListAnalyzer(
            com.recred.backend.infrastructure.gemini.GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        return new GeminiSupplierPriceListAnalyzer(geminiTextClient, objectMapper);
    }

    @Bean
    public CreateSupplier createSupplier(ProveedorRepository proveedorRepository, Clock clock) {
        return new CreateSupplier(proveedorRepository, clock);
    }

    @Bean
    public ListSuppliers listSuppliers(ProveedorRepository proveedorRepository) {
        return new ListSuppliers(proveedorRepository);
    }

    @Bean
    public GetSupplier getSupplier(ProveedorRepository proveedorRepository) {
        return new GetSupplier(proveedorRepository);
    }

    @Bean
    public UpdateSupplier updateSupplier(ProveedorRepository proveedorRepository) {
        return new UpdateSupplier(proveedorRepository);
    }

    @Bean
    public DeleteSupplier deleteSupplier(ProveedorRepository proveedorRepository) {
        return new DeleteSupplier(proveedorRepository);
    }

    @Bean
    public UploadSupplierPriceList uploadSupplierPriceList(
            ProveedorRepository proveedorRepository,
            ListaPrecioProveedorRepository listaPrecioProveedorRepository,
            ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository,
            ProductRepository productRepository,
            StorageService storageService,
            SupplierPriceListAnalyzer supplierPriceListAnalyzer) {
        return new UploadSupplierPriceList(
                proveedorRepository,
                listaPrecioProveedorRepository,
                itemListaPrecioProveedorRepository,
                productRepository,
                storageService,
                supplierPriceListAnalyzer);
    }

    @Bean
    public UpdatePriceListItemMapping updatePriceListItemMapping(
            ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository,
            ListaPrecioProveedorRepository listaPrecioProveedorRepository,
            ProveedorRepository proveedorRepository) {
        return new UpdatePriceListItemMapping(
                itemListaPrecioProveedorRepository,
                listaPrecioProveedorRepository,
                proveedorRepository);
    }

    @Bean
    public RecommendSupplierForProducts recommendSupplierForProducts(
            ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository,
            ProveedorRepository proveedorRepository,
            ProductRepository productRepository) {
        return new RecommendSupplierForProducts(
                itemListaPrecioProveedorRepository,
                proveedorRepository,
                productRepository);
    }

    @Bean
    public SchoolAssistantRoleCatalog schoolAssistantRoleCatalog() {
        return new SchoolAssistantRoleCatalog();
    }

    @Bean
    public StudentAssistantContextBuilder studentAssistantContextBuilder(
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository schoolAssistantPurchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            SchoolAssistantRoleCatalog roleCatalog,
            Clock clock) {
        return new StudentAssistantContextBuilder(
                walletRepository,
                paymentRepository,
                schoolAssistantPurchaseRepository,
                schoolCalendarRepository,
                roleCatalog,
                clock);
    }

    @Bean
    public StudentAssistantContextProvider studentAssistantContextProvider(
            AlumnoRepository alumnoRepository,
            StudentAssistantContextBuilder contextBuilder) {
        return new StudentAssistantContextProvider(alumnoRepository, contextBuilder);
    }

    @Bean
    public TutorAssistantContextBuilder tutorAssistantContextBuilder(
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            PurchaseRepository purchaseRepository,
            PresupuestoRepository presupuestoRepository,
            SchoolAssistantRoleCatalog roleCatalog,
            Clock clock) {
        return new TutorAssistantContextBuilder(
                paymentRepository,
                schoolCalendarRepository,
                purchaseRepository,
                presupuestoRepository,
                roleCatalog,
                clock);
    }

    @Bean
    public TutorAssistantContextProvider tutorAssistantContextProvider(
            TutorRepository tutorRepository,
            TutorAssistantContextBuilder contextBuilder) {
        return new TutorAssistantContextProvider(tutorRepository, contextBuilder);
    }

    @Bean
    public KiosqueroAssistantContextBuilder kiosqueroAssistantContextBuilder(
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            SchoolAssistantRoleCatalog roleCatalog) {
        return new KiosqueroAssistantContextBuilder(
                productRepository,
                purchaseRepository,
                roleCatalog);
    }

    @Bean
    public KiosqueroAssistantContextProvider kiosqueroAssistantContextProvider(
            KiosqueroRepository kiosqueroRepository,
            KiosqueroAssistantContextBuilder contextBuilder) {
        return new KiosqueroAssistantContextProvider(kiosqueroRepository, contextBuilder);
    }

    @Bean
    public SchoolAssistantContextResolver schoolAssistantContextResolver(
            UsuarioRepository usuarioRepository,
            StudentAssistantContextProvider studentProvider,
            TutorAssistantContextProvider tutorProvider,
            KiosqueroAssistantContextProvider kiosqueroProvider) {
        List<SchoolAssistantContextProvider> providers = List.of(
                studentProvider,
                tutorProvider,
                kiosqueroProvider
        );
        return new SchoolAssistantContextResolver(usuarioRepository, providers);
    }

    @Bean
    public SchoolAssistantIntentDetector schoolAssistantIntentDetector() {
        return new SchoolAssistantIntentDetector();
    }

    @Bean
    public SchoolAssistantReplyFormatter schoolAssistantReplyFormatter() {
        return new SchoolAssistantReplyFormatter();
    }

    @Bean
    public SchoolAssistantSuggestionFactory schoolAssistantSuggestionFactory() {
        return new SchoolAssistantSuggestionFactory();
    }

    @Bean
    public SchoolAssistantReplyBuilder schoolAssistantReplyBuilder(
            SchoolAssistantSuggestionFactory suggestionFactory) {
        return new SchoolAssistantReplyBuilder(suggestionFactory);
    }

    @Bean
    public SchoolAssistantAnswerFormatter schoolAssistantAnswerFormatter() {
        return new SchoolAssistantAnswerFormatter();
    }

    @Bean
    public SchoolAssistantTutorFactsFormatter schoolAssistantTutorFactsFormatter() {
        return new SchoolAssistantTutorFactsFormatter();
    }

    @Bean
    public SchoolAssistantContentFormatter schoolAssistantContentFormatter(
            ProductRepository productRepository,
            SchoolAssistantReplyFormatter replyFormatter) {
        return new SchoolAssistantContentFormatter(productRepository, replyFormatter);
    }

    @Bean
    public SchoolAssistantQuickPurchaseReplyFactory schoolAssistantQuickPurchaseReplyFactory(
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantSuggestionFactory suggestionFactory,
            SchoolAssistantReplyBuilder replyBuilder) {
        return new SchoolAssistantQuickPurchaseReplyFactory(
                replyFormatter,
                contentFormatter,
                suggestionFactory,
                replyBuilder);
    }

    @Bean
    public SchoolAssistantPendingPurchaseParser schoolAssistantPendingPurchaseParser(
            SchoolAssistantIntentDetector intentDetector) {
        return new SchoolAssistantPendingPurchaseParser(intentDetector);
    }

    @Bean
    public SchoolAssistantPurchaseFailureMessages schoolAssistantPurchaseFailureMessages() {
        return new SchoolAssistantPurchaseFailureMessages();
    }

    @Bean
    public SchoolAssistantPurchasePatternService schoolAssistantPurchasePatternService(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            SchoolAssistantContentFormatter contentFormatter) {
        return new SchoolAssistantPurchasePatternService(
                purchaseRepository,
                productRepository,
                contentFormatter);
    }

    @Bean
    public SchoolAssistantPendingPurchaseResponder schoolAssistantPendingPurchaseResponder(
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            Clock clock,
            SchoolAssistantPurchaseFailureMessages purchaseFailureMessages,
            SchoolAssistantPendingPurchaseParser pendingPurchaseParser,
            SchoolAssistantQuickPurchaseReplyFactory replyFactory) {
        return new SchoolAssistantPendingPurchaseResponder(
                productRepository,
                createAssistantAdvancePurchase,
                clock,
                purchaseFailureMessages,
                pendingPurchaseParser,
                replyFactory);
    }

    @Bean
    public SchoolAssistantQuickPurchaseHandler schoolAssistantQuickPurchaseHandler(
            SchoolAssistantPurchasePatternService purchasePatternService,
            TutorRepository tutorRepository,
            Clock clock,
            SchoolAssistantQuickPurchaseReplyFactory replyFactory,
            SchoolAssistantPendingPurchaseResponder pendingPurchaseResponder) {
        return new SchoolAssistantQuickPurchaseHandler(
                purchasePatternService,
                tutorRepository,
                clock,
                replyFactory,
                pendingPurchaseResponder);
    }

    @Bean
    public SchoolAssistantPurchaseRequestParser schoolAssistantPurchaseRequestParser(
            SchoolAssistantIntentDetector intentDetector,
            Clock clock) {
        return new SchoolAssistantPurchaseRequestParser(intentDetector, clock);
    }

    @Bean
    public SchoolAssistantPurchaseResponder schoolAssistantPurchaseResponder(
            SchoolAssistantPurchaseRequestParser requestParser,
            SchoolAssistantQuickPurchaseHandler quickPurchaseHandler,
            SchoolAssistantReplyBuilder replyBuilder) {
        return new SchoolAssistantPurchaseResponder(
                requestParser,
                quickPurchaseHandler,
                replyBuilder);
    }

    @Bean
    public SchoolAssistantPurchaseCancellationHandler schoolAssistantPurchaseCancellationHandler(
            PurchaseRepository purchaseRepository,
            CancelPresentialPurchase cancelPresentialPurchase,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantReplyBuilder replyBuilder) {
        return new SchoolAssistantPurchaseCancellationHandler(
                purchaseRepository,
                cancelPresentialPurchase,
                contentFormatter,
                replyBuilder);
    }

    @Bean
    public SchoolAssistantPurchaseCancellationResponder
            schoolAssistantPurchaseCancellationResponder(
                    SchoolAssistantIntentDetector intentDetector,
                    SchoolAssistantPurchaseCancellationHandler purchaseCancellationHandler) {
        return new SchoolAssistantPurchaseCancellationResponder(
                intentDetector,
                purchaseCancellationHandler);
    }

    @Bean
    public SchoolAssistantFixedTopicResponses schoolAssistantFixedTopicResponses(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            SchoolAssistantPurchasePatternService purchasePatternService,
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantTutorFactsFormatter tutorFactsFormatter,
            SchoolAssistantReplyBuilder replyBuilder) {
        return new SchoolAssistantFixedTopicResponses(
                purchaseRepository,
                productRepository,
                purchasePatternService,
                replyFormatter,
                contentFormatter,
                tutorFactsFormatter,
                replyBuilder);
    }

    @Bean
    public SchoolAssistantFixedTopics schoolAssistantFixedTopics(
            SchoolAssistantIntentDetector intentDetector,
            SchoolAssistantFixedTopicResponses responses) {
        return new SchoolAssistantFixedTopics(intentDetector, responses);
    }

    @Bean
    public SchoolAssistantConversationService schoolAssistantConversationService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository) {
        return new SchoolAssistantConversationService(
                chatSessionRepository,
                chatMessageRepository);
    }

    @Bean
    public SchoolAssistantQuestionResponder schoolAssistantQuestionResponder(
            SchoolAssistantFixedTopics fixedTopics,
            SchoolAssistantResponder schoolAssistantResponder,
            SchoolAssistantAnswerFormatter answerFormatter,
            SchoolAssistantRoleCatalog roleCatalog) {
        return new SchoolAssistantQuestionResponder(
                fixedTopics,
                schoolAssistantResponder,
                answerFormatter,
                roleCatalog);
    }

    @Bean
    public SchoolAssistantMessageClassifier schoolAssistantMessageClassifier(
            SchoolAssistantIntentDetector intentDetector) {
        return new SchoolAssistantMessageClassifier(intentDetector);
    }

    @Bean
    public AskSchoolAssistant askSchoolAssistant(
            SchoolAssistantConversationService conversationService,
            SchoolAssistantContextResolver contextResolver,
            SchoolAssistantQuestionResponder questionResponder,
            SchoolAssistantMessageClassifier messageClassifier,
            SchoolAssistantPurchaseResponder purchaseResponder,
            SchoolAssistantPurchaseCancellationResponder purchaseCancellationResponder,
            Clock clock) {
        return new AskSchoolAssistant(
                conversationService,
                contextResolver,
                questionResponder,
                messageClassifier,
                purchaseResponder,
                purchaseCancellationResponder,
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
    public SaveKiosqueroPayoutConfig saveKiosqueroPayoutConfig(
            KiosqueroPayoutConfigRepository repository,
            Clock clock) {
        return new SaveKiosqueroPayoutConfig(repository, clock);
    }

    @Bean
    public GetKiosqueroPayoutConfig getKiosqueroPayoutConfig(
            KiosqueroPayoutConfigRepository repository) {
        return new GetKiosqueroPayoutConfig(repository);
    }

    @Bean
    public ProcessScheduledPayouts processScheduledPayouts(
            KiosqueroPayoutConfigRepository repository,
            PayoutPaymentService paymentService,
            KiosqueroRepository kiosqueroRepository,
            PurchaseRepository purchaseRepository,
            Clock clock) {
        return new ProcessScheduledPayouts(repository, paymentService,
                kiosqueroRepository, purchaseRepository, clock);
    }
}
