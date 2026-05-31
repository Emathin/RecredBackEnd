package com.recred.backend.config;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.HealthClassificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.TimeBadgeRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.MerchantRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.BudgetRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import com.recred.backend.domain.repositories.UserRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.BudgetPeriodCalculator;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import com.recred.backend.domain.services.UserService;
import com.recred.backend.domain.services.RestrictionsValidatorService;
import com.recred.backend.domain.services.TimeValidatorService;
import com.recred.backend.domain.services.BudgetValidatorService;
import com.recred.backend.infrastructure.gemini.GeminiSeasonalSuggester;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCases {

    @Bean
    public GeneratePriceAlert generatePriceAlert(
            StudentAIProfileRepository studentAIProfileRepository,
            PriceAlertAIRepository priceAlertAIRepository,
            AISuggestionRepository AISuggestionRepository,
            Clock clock) {
        return new GeneratePriceAlert(studentAIProfileRepository,
                priceAlertAIRepository, AISuggestionRepository, clock);
    }

    @Bean
    public UpdateProductPrice updateProductPrice(
            ProductRepository productRepository,
            ProductPriceChangedPublisher eventPublisher) {
        return new UpdateProductPrice(productRepository, eventPublisher);
    }

    @Bean
    public BudgetPeriodCalculator calculadorPeriodoPresupuesto() {
        return new BudgetPeriodCalculator();
    }

    @Bean
    public RestrictionsValidatorService validacionRestriccionesService() {
        return new RestrictionsValidatorService();
    }

    @Bean
    public UpdateBudgetRules updateBudgetRules(BudgetRepository repository) {
        return new UpdateBudgetRules(repository);
    }

    @Bean
    public TimeValidatorService validadorHorarioService() {
        return new TimeValidatorService();
    }

    @Bean
    public BudgetValidatorService validadorPresupuestoService(
            BudgetRepository budgetRepository,
            PurchaseRepository purchaseRepository,
            BudgetPeriodCalculator calculadorPeriodo) {
        return new BudgetValidatorService(
                budgetRepository,
                purchaseRepository,
                calculadorPeriodo);
    }

    @Bean
    public UserService usuarioService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public AskSchoolAssistant askSchoolAssistant(
            StudentRepository studentRepository,
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository purchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SchoolAssistantResponder schoolAssistantResponder,
            Clock clock) {
        return new AskSchoolAssistant(
                studentRepository,
                walletRepository,
                paymentRepository,
                purchaseRepository,
                schoolCalendarRepository,
                chatSessionRepository,
                chatMessageRepository,
                schoolAssistantResponder,
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
    public ProcessPresentialPurchase processPresentialPurchase(
            StudentRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            TimeValidatorService timeValidator,
            RestrictionsValidatorService healthValidator,
            BudgetValidatorService budgetValidator) {
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
            StudentRepository studentRepository,
            HealthClassificationRepository clasificacionRepository) {
        return new ManageRestrictions(studentRepository, clasificacionRepository);
    }

    @Bean
    public DetectStudentPreferences detectStudentPreferences(
            StudentRepository studentRepository,
            PreferenceAIRepository preferenceAIRepository,
            AISuggestionRepository AISuggestionRepository,
            PurchaseRepository purchaseRepository,
            InventoryRepository inventoryRepository) {
        return new DetectStudentPreferences(studentRepository, preferenceAIRepository,
                AISuggestionRepository, purchaseRepository, inventoryRepository);
    }

    @Bean
    public GetHealthRestrictions obtenerRestriccionesSalud(
            StudentRepository studentRepository) {
        return new GetHealthRestrictions(studentRepository);
    }

    @Bean
    public AddHealthRestriction agregarRestriccionSalud(
            StudentRepository studentRepository,
            HealthClassificationRepository clasificacionRepository) {
        return new AddHealthRestriction(studentRepository, clasificacionRepository);
    }

    @Bean
    public RemoveHealthRestrictions eliminarRestriccionSalud(
            StudentRepository studentRepository) {
        return new RemoveHealthRestrictions(studentRepository);
    }

    @Bean
    public GetHealthRestrictionsCatalog obtenerCatalogoRestriccionesSalud(
            HealthClassificationRepository healthClassificationRepository) {
        return new GetHealthRestrictionsCatalog(healthClassificationRepository);
    }

    @Bean
    public GetTimeSlotsBySchool getTimeSlotsBySchool(
            TimeBadgeRepository timeBadgeRepository) {
        return new GetTimeSlotsBySchool(timeBadgeRepository);
    }

    @Bean
    public CreateTimeRestriction createTimeRestriction(
            TimeRestrictionRepository timeRestrictionRepository) {
        return new CreateTimeRestriction(timeRestrictionRepository);
    }

    @Bean
    public DisableTimeRestriction disableTimeRestriction(
            TimeRestrictionRepository timeRestrictionRepository) {
        return new DisableTimeRestriction(timeRestrictionRepository);
    }

    @Bean
    public GetTimeRestrictionsByStudent getTimeRestrictionsByStudent(
            TimeRestrictionRepository timeRestrictionRepository) {
        return new GetTimeRestrictionsByStudent(timeRestrictionRepository);
    }

    @Bean
    public UpdateTimeRestriction updateTimeRestriction(
            TimeRestrictionRepository timeRestrictionRepository) {
        return new UpdateTimeRestriction(timeRestrictionRepository);
    }

    @Bean
    public GetStudentPreferences obtenerPreferenciasAlumno(
            AISuggestionRepository AISuggestionRepository) {
        return new GetStudentPreferences(AISuggestionRepository);
    }

    @Bean
    public RespondToAISuggestion responderSugerenciaIA(
            AISuggestionRepository AISuggestionRepository) {
        return new RespondToAISuggestion(AISuggestionRepository);
    }

    @Bean
    public PredictStudentSpending predictStudentSpending(
            StudentRepository studentRepository,
            SpendingPreferenceRepository spendingPreferenceRepository,
            PurchaseSpendingRepository purchaseSpendingRepository,
            SpendingPredictionAnalyzer spendingPredictionAnalyzer,
            AIEventRepository aiEventRepository,
            SpendingPredictionRangeService spendingPredictionRangeService,
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock) {
        return new PredictStudentSpending(
                studentRepository,
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
            BudgetRepository budgetRepository) {
        return new GetActiveBudgetByStudent(budgetRepository);
    }

    @Bean
    public CreateBudget createBudget(
            BudgetRepository budgetRepository) {
        return new CreateBudget(budgetRepository);
    }

    @Bean
    public UpdateBudget updateBudget(
            BudgetRepository budgetRepository) {
        return new UpdateBudget(budgetRepository);
    }

    @Bean
    public DisableBudget disableBudget(
            BudgetRepository budgetRepository) {
        return new DisableBudget(budgetRepository);
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
    public GenerateProductChangeRecommendation generateProductChangeRecommendation(
            ProductRepository productRepository, ProductChangeAnalyzer productChangeAnalyzer,
            AIEventRepository aiEventRepository, MerchantRepository merchantRepository) {
        return new GenerateProductChangeRecommendation(
                productRepository, productChangeAnalyzer, aiEventRepository, merchantRepository);
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
            MerchantRepository merchantRepository,
            InventoryRepository inventoryRepository,
            AIEventRepository aiEventRepository) {
        return new AddProductRecommendation(
                productRepository,
                merchantRepository,
                inventoryRepository,
                aiEventRepository);
    }

}
