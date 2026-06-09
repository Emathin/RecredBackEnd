package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.BloquearProductoManual;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DesbloquearProductoManual;
import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.GenerarMenuBuffet;
import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetCartSuggestions;
import com.recred.backend.application.GetFavoritosByAlumno;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetLoggedUserNotifications;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ObtenerHijosPorPadre;
import com.recred.backend.application.ObtenerHistorialAlumno;
import com.recred.backend.application.ObtenerHistorialTutor;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.PurchaseValidatorImpl;
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
    public GenerateParentSummary generateParentSummary(
            TutorRepository tutorRepository,
            PurchaseRepository purchaseRepository,
            SummaryRepository summaryRepository,
            ParentSummaryAnalyzer parentSummaryAnalyzer) {
        return new GenerateParentSummary(tutorRepository,
                purchaseRepository, summaryRepository, parentSummaryAnalyzer);
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
    public CreateAdvancePurchase createAdvancePurchase(PurchaseValidator purchaseValidator,
                                                       AlumnoRepository alumnoRepository,
                                                       ProductRepository productRepository,
                                                       PurchaseRepository purchaseRepository,
                                                       WithdrawalCodeService withdrawalCodeService,
                                                       WalletRepository walletRepository,
                                                       FranjaHorariaRepository timeSlotRepository,
                                                       SpringDataBuffetRepository buffetRepository,
                                                       UsuarioRepository usuarioRepository) {
        return new CreateAdvancePurchase(
                purchaseValidator,
                alumnoRepository,
                productRepository,
                purchaseRepository,
                withdrawalCodeService,
                walletRepository,
                timeSlotRepository,
                buffetRepository,
                usuarioRepository
        );
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
            PurchaseValidator purchaseValidator) {
        return new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator);
    }

    @Bean
    public CreateAssistantAdvancePurchase createAssistantAdvancePurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            FranjaHorariaRepository timeSlotRepository,
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator,
            WithdrawalCodeService withdrawalCodeService,
            Clock clock) {
        return new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                timeSlotRepository,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                clock);
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
    public UpdatePurchaseStatus updatePurchaseStatus(PurchaseRepository purchaseRepository) {
        return new UpdatePurchaseStatus(purchaseRepository);
    }

    @Bean
    public CancelPresentialPurchase cancelPurchase(PurchaseRepository purchaseRepository,
            WalletRepository walletRepository) {
        return new CancelPresentialPurchase(purchaseRepository, walletRepository);
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
    public LoadStockIA loadStockIA(LoadStockIAService loadStockIAService) {
        return new LoadStockIA(loadStockIAService);
    }

    @Bean
    public SaveProduct saveProduct(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository) {
        return new SaveProduct(productRepository, inventoryRepository, categoryRepository);
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
            AIEventRepository aiEventRepository, KiosqueroRepository kiosqueroRepository) {
        return new GenerateProductChangeRecommendation(
                productRepository, productChangeAnalyzer, aiEventRepository, kiosqueroRepository);
    }

    @Bean
    public GenerateSeasonalRecommendation generateSeasonalRecommendation(
            GeminiSeasonalSuggester seasonalSuggester, OpenMeteoWeatherClient weatherClient) {
        return new GenerateSeasonalRecommendation(
                weatherClient, seasonalSuggester);
    }

    @Bean
    public AddProductRecommendation addProductRecommendation(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository,
            InventoryRepository inventoryRepository,
            AIEventRepository aiEventRepository) {
        return new AddProductRecommendation(
                productRepository,
                kiosqueroRepository,
                inventoryRepository,
                aiEventRepository);
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
            WalletRepository walletRepository) {
        return new LoadBalanceToChild(usuarioRepository, walletRepository);
    }

    @Bean
    public TransferBalanceBetweenChildren transferBalanceBetweenChildren(
            UsuarioRepository usuarioRepository,
            WalletRepository walletRepository) {
        return new TransferBalanceBetweenChildren(usuarioRepository, walletRepository);
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
}
