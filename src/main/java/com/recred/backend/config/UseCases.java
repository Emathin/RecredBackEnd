package com.recred.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.*;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.repositories.*;
import com.recred.backend.domain.services.*;
import com.recred.backend.infrastructure.gemini.*;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCases {

    @Bean
    public GenerateParentSummary generateParentSummary(
            TutorRepository tutorRepository,
            PurchaseRepository purchaseRepository,
            SummaryRepository summaryRepository) {
        return new GenerateParentSummary(tutorRepository,
                purchaseRepository, summaryRepository);
    }

    @Bean
    public WithdrawalCodeServiceImpl withdrawalCodeServiceImpl() {
        return new WithdrawalCodeServiceImpl();
    }

    @Bean
    public PurchaseValidatorImpl purchaseValidatorImpl(
            ValidadorHorarioService validadorHorarioService,
            ValidacionRestriccionesService validacionRestriccionesService,
            ValidadorPresupuestoService validadorPresupuestoService
    ) {
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
            ValidadorPresupuestoService validadorPresupuestoService
    ){
        return new PurchaseValidatorImpl(
                validadorHorarioService,
                validacionRestriccionesService,
                validadorPresupuestoService
        );
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
                                                       WithdrawalCodeService withdrawalCodeService){
        return new CreateAdvancePurchase( purchaseValidator,
                alumnoRepository,
                productRepository,
                purchaseRepository,
                withdrawalCodeService);
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
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator) {
        return new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                timeValidator,
                healthValidator,
                budgetValidator);
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
            RestriccionHorariaRepository restriccionHorariaRepository) {
        return new CreateTimeRestriction(restriccionHorariaRepository);
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
                                               CalculadorPeriodoPresupuesto calculadorPeriodo){
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
            RestriccionHorariaRepository restriccionHorariaRepository) {
        return new UpdateTimeRestriction(restriccionHorariaRepository);
    }

    @Bean
    public GetStudentPreferences obtenerPreferenciasAlumno(
            SugerenciaIARepository sugerenciaIARepository) {
        return new GetStudentPreferences(sugerenciaIARepository);
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
            GeminiSeasonalSuggester seasonalSuggester, OpenMeteoWeatherClient weatherClient){
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
            AlumnoRepository alumnoRepository,PurchaseRepository purchaseRepository,
            PurchaseAnalyzer purchaseAnalyzer,AIEventRepository aiEventRepository,
            PurchaseRecommendationNotificationRepository notificationRepository,
            ConsumptionAlertStudentRepository studentRepository,
            SuggestionAiRepository suggestionAiRepository) {
        return new GeneratePurchaseRecommendation(alumnoRepository,
                purchaseRepository,purchaseAnalyzer,aiEventRepository, notificationRepository,
                studentRepository,suggestionAiRepository);
    }

    @Bean
    public GetProductsByBuffetUser getProductsByBuffetUser(
            ProductRepository productRepository,
            KiosqueroRepository kiosqueroRepository){
        return new GetProductsByBuffetUser(
                productRepository, kiosqueroRepository);
    }
}
