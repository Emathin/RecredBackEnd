package com.recred.backend.unit.application;

import com.recred.backend.application.PrizeHealthyPurchase;
import com.recred.backend.application.PrizeHealthyPurchaseRequest;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrizeHealthyPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    @Captor
    private ArgumentCaptor<Alumno> studentCaptor;

    private PrizeHealthyPurchase prizeHealthyPurchase;
    private PrizeHealthyPurchaseRequest prizeHealthyPurchaseRequest;
    private Throwable thrownException;
    private UUID studentId;
    private UUID purchaseId;
    private int expectedRewardPoints;

    @BeforeEach
    void setUp() {
        prizeHealthyPurchase = new PrizeHealthyPurchase(purchaseRepository, alumnoRepository);
    }

    @Test
    void givenFirstHealthyPurchase_whenRewardingHealthyPurchase_thenRewardsBeginnerLevel() {
        givenFirstHealthyPurchaseScenario();

        whenPrizeHealthyPurchase();

        thenStudentWasRewarded();
    }

    @Test
    void givenThreeHealthyPurchases_whenRewardingHealthyPurchase_thenRewardsCrackLevel() {
        givenCrackHealthyPurchaseScenario();

        whenPrizeHealthyPurchase();

        thenStudentWasRewarded();
    }

    @Test
    void givenMoreThanFiveHealthyPurchasesInFifteenDays_whenRewarding_thenRewardsGoatLevel() {
        givenGoatHealthyPurchaseScenario();

        whenPrizeHealthyPurchase();

        thenStudentWasRewarded();
    }

    @Test
    void givenPurchaseWithoutSuggestion_whenRewardingHealthyPurchase_thenDoesNothing() {
        givenPurchaseWithoutSuggestionScenario();

        whenPrizeHealthyPurchase();

        thenNoRewardWasPersisted();
    }

    @Test
    void givenMissingPurchase_whenRewardingHealthyPurchase_thenThrowsException() {
        givenMissingPurchaseScenario();

        whenPrizeHealthyPurchaseFails(PurchaseNotFoundException.class);

        thenErrorMessageIs("No existe la compra con id " + purchaseId);
        thenNoRewardWasPersistedAfterMissingPurchase();
    }

    @Test
    void givenMissingStudent_whenRewardingHealthyPurchase_thenThrowsException() {
        givenMissingStudentScenario();

        whenPrizeHealthyPurchaseFails(AlumnoNoEncontradoException.class);

        thenErrorMessageIs("No existe el alumno con id " + studentId);
        thenStudentWasNotSaved();
    }

    private void givenFirstHealthyPurchaseScenario() {
        givenRequest();
        givenPurchaseWithSuggestion();
        givenHealthyPurchases(1L);
        givenStudentWithPoints(0);
        expectedRewardPoints = RewardLevel.PRINCIPIANTE.getPoints();
    }

    private void givenCrackHealthyPurchaseScenario() {
        givenRequest();
        givenPurchaseWithSuggestion();
        givenHealthyPurchases(3L);
        givenStudentWithPoints(10);
        expectedRewardPoints = 10 + RewardLevel.CRACK.getPoints();
    }

    private void givenGoatHealthyPurchaseScenario() {
        givenRequest();
        givenPurchaseWithSuggestion();
        givenHealthyPurchasesForGoatLevel();
        givenStudentWithPoints(50);
        expectedRewardPoints = 50 + RewardLevel.GOAT.getPoints();
    }

    private void givenPurchaseWithoutSuggestionScenario() {
        givenRequest();
        Purchase purchase = purchaseWithoutSuggestion();
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenMissingPurchaseScenario() {
        givenRequest();
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenMissingStudentScenario() {
        givenRequest();
        givenPurchaseWithSuggestion();
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenRequest() {
        studentId = randomUUID();
        purchaseId = randomUUID();
        prizeHealthyPurchaseRequest = new PrizeHealthyPurchaseRequest(studentId, purchaseId);
    }

    private void givenPurchaseWithSuggestion() {
        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(purchaseWithSuggestion()));
    }

    private void givenHealthyPurchases(long count) {
        when(purchaseRepository.countHealthyPurchasesByAlumno(eq(studentId), any()))
                .thenReturn(count);
    }

    private void givenHealthyPurchasesForGoatLevel() {
        when(purchaseRepository.countHealthyPurchasesByAlumno(
                eq(studentId),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            LocalDateTime since = invocation.getArgument(1);
            if (since.isAfter(LocalDateTime.now().minusDays(8))) {
                return 2L;
            }
            return 6L;
        });
    }

    private void givenStudentWithPoints(int points) {
        when(alumnoRepository.findById(studentId))
                .thenReturn(Optional.of(studentWithPoints(points)));
    }

    private void whenPrizeHealthyPurchase() {
        prizeHealthyPurchase.execute(prizeHealthyPurchaseRequest);
    }

    private <T extends Throwable> void whenPrizeHealthyPurchaseFails(
            Class<T> expectedException
    ) {
        Executable execution = () -> prizeHealthyPurchase.execute(prizeHealthyPurchaseRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenStudentWasRewarded() {
        verify(alumnoRepository).save(studentCaptor.capture());
        assertEquals(expectedRewardPoints, studentCaptor.getValue().getPuntosRecompensa());
    }

    private void thenNoRewardWasPersisted() {
        verify(purchaseRepository, never()).countHealthyPurchasesByAlumno(any(), any());
        verify(alumnoRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    private void thenNoRewardWasPersistedAfterMissingPurchase() {
        verify(purchaseRepository, never()).countHealthyPurchasesByAlumno(any(), any());
        verify(alumnoRepository, never()).save(any());
    }

    private void thenStudentWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Purchase purchaseWithSuggestion() {
        Purchase purchase = new Purchase();
        purchase.setSuggestionId(randomUUID());
        return purchase;
    }

    private Purchase purchaseWithoutSuggestion() {
        Purchase purchase = new Purchase();
        purchase.setSuggestionId(null);
        return purchase;
    }

    private Alumno studentWithPoints(int rewardPoints) {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setPuntosRecompensa(rewardPoints);
        return student;
    }
}
