package com.recred.backend.config;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;

import java.time.Clock;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCases {

    @Bean
    public CalculadorPeriodoPresupuesto calculadorPeriodoPresupuesto() {
        return new CalculadorPeriodoPresupuesto();
    }

    @Bean
    public ValidacionRestriccionesService validacionRestriccionesService() {
        return new ValidacionRestriccionesService();
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
                calculadorPeriodo
        );
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
                budgetValidator
        );
    }

    @Bean
    public GetPresentialPurchase getPurchase(PurchaseRepository purchaseRepository) {
        return new GetPresentialPurchase(purchaseRepository);
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
    public PredictStudentSpending predictStudentSpending(
            AlumnoRepository alumnoRepository,
            SpendingPreferenceRepository spendingPreferenceRepository,
            PurchaseSpendingRepository purchaseSpendingRepository,
            SpendingPredictionAnalyzer spendingPredictionAnalyzer,
            AIEventRepository aiEventRepository,
            SpendingPredictionRangeService spendingPredictionRangeService,
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock
    ) {
        return new PredictStudentSpending(
                alumnoRepository,
                spendingPreferenceRepository,
                purchaseSpendingRepository,
                spendingPredictionAnalyzer,
                aiEventRepository,
                spendingPredictionRangeService,
                spendingPredictionCalculator,
                clock
        );
    }

    @Bean
    public GenerateStatisticalConsumptionAlerts generateStatisticalConsumptionAlerts(
            ConsumptionAlertStudentRepository studentRepository,
            ConsumptionAlertPurchaseRepository purchaseRepository,
            ConsumptionAlertNotificationRepository notificationRepository,
            ConsumptionAlertAnalyzer analyzer,
            StatisticalConsumptionAlertProperties properties,
            Clock clock
    ) {
        return new GenerateStatisticalConsumptionAlerts(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties,
                clock
        );
    }

    @Bean
    public GenerateHealthyProductRecommendations generateHealthyProductRecommendations(
            ConsumptionAlertStudentRepository studentRepository,
            HealthyRecommendationPurchaseRepository purchaseRepository,
            HealthyRecommendationNotificationRepository notificationRepository,
            HealthyRecommendationAnalyzer analyzer,
            HealthyRecommendationProperties properties,
            Clock clock
    ) {
        return new GenerateHealthyProductRecommendations(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties,
                clock
        );
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


}
