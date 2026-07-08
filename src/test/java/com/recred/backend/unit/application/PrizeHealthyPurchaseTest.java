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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrizeHealthyPurchaseTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
    private final PrizeHealthyPurchase useCase = new PrizeHealthyPurchase(
            purchaseRepository,
            alumnoRepository
    );

    private UUID studentId;
    private UUID purchaseId;
    private Alumno student;
    private RuntimeException exception;

    @Test
    void execute_WhenSuggestedPurchaseIsFirstHealthyPurchase_RewardsStudentWithBronze() {
        givenSuggestedPurchase();
        givenStudentWithRewardPoints(0);
        givenHealthyPurchaseCounts(1, 1);

        whenPrizingHealthyPurchase();

        thenStudentWasSavedWithRewardPoints(RewardLevel.BRONZE.getPoints());
        thenHealthyPurchasesWereCountedForRewardLevel();
    }

    @Test
    void execute_WhenStudentHasThreeHealthyPurchasesInSevenDays_RewardsStudentWithSilver() {
        givenSuggestedPurchase();
        givenStudentWithRewardPoints(10);
        givenHealthyPurchaseCounts(3, 3);

        whenPrizingHealthyPurchase();

        thenStudentWasSavedWithRewardPoints(10 + RewardLevel.SILVER.getPoints());
        thenHealthyPurchasesWereCountedForRewardLevel();
    }

    @Test
    void execute_WhenStudentHasMoreThanFiveHealthyPurchasesInFifteenDays_RewardsStudentWithGold() {
        givenSuggestedPurchase();
        givenStudentWithRewardPoints(50);
        givenHealthyPurchaseCounts(2, 6);

        whenPrizingHealthyPurchase();

        thenStudentWasSavedWithRewardPoints(50 + RewardLevel.GOLD.getPoints());
        thenHealthyPurchasesWereCountedForRewardLevel();
    }

    @Test
    void execute_WhenPurchaseHasNoSuggestion_DoesNotRewardStudent() {
        givenPurchaseWithoutSuggestion();

        whenPrizingHealthyPurchase();

        thenStudentWasNotRequested();
        thenRewardWasNotCalculated();
        thenStudentWasNotSaved();
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ThrowsException() {
        givenMissingPurchase();

        whenPrizingHealthyPurchaseFails(PurchaseNotFoundException.class);

        thenExceptionMessageIs("No existe la compra con id " + purchaseId);
        thenStudentWasNotRequested();
    }

    @Test
    void execute_WhenStudentDoesNotExist_ThrowsException() {
        givenSuggestedPurchase();
        givenMissingStudent();

        whenPrizingHealthyPurchaseFails(AlumnoNoEncontradoException.class);

        thenExceptionMessageIs("No existe el alumno con id " + studentId);
        thenRewardWasNotCalculated();
        thenStudentWasNotSaved();
    }

    private void givenSuggestedPurchase() {
        studentId = randomUUID();
        purchaseId = randomUUID();
        Purchase purchase = purchaseWithSuggestion(randomUUID());
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseWithoutSuggestion() {
        studentId = randomUUID();
        purchaseId = randomUUID();
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(new Purchase()));
    }

    private void givenMissingPurchase() {
        studentId = randomUUID();
        purchaseId = randomUUID();
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenStudentWithRewardPoints(int rewardPoints) {
        student = student(rewardPoints);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenMissingStudent() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenHealthyPurchaseCounts(long last7Days, long last15Days) {
        when(purchaseRepository.countHealthyPurchasesByAlumno(eq(studentId), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime since = invocation.getArgument(1);
                    if (since.isAfter(LocalDateTime.now().minusDays(8))) {
                        return last7Days;
                    }
                    return last15Days;
                });
    }

    private void whenPrizingHealthyPurchase() {
        useCase.execute(defaultRequest());
    }

    private <T extends RuntimeException> void whenPrizingHealthyPurchaseFails(
            Class<T> expectedExceptionType
    ) {
        Executable execution = () -> useCase.execute(defaultRequest());

        exception = assertThrows(expectedExceptionType, execution);
    }

    private void thenStudentWasSavedWithRewardPoints(int expectedRewardPoints) {
        ArgumentCaptor<Alumno> studentCaptor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(studentCaptor.capture());
        assertEquals(expectedRewardPoints, studentCaptor.getValue().getPuntosRecompensa());
    }

    private void thenHealthyPurchasesWereCountedForRewardLevel() {
        verify(purchaseRepository, times(2)).countHealthyPurchasesByAlumno(eq(studentId), any());
    }

    private void thenStudentWasNotRequested() {
        verifyNoInteractions(alumnoRepository);
    }

    private void thenRewardWasNotCalculated() {
        verify(purchaseRepository, never()).countHealthyPurchasesByAlumno(any(), any());
    }

    private void thenStudentWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private PrizeHealthyPurchaseRequest defaultRequest() {
        return new PrizeHealthyPurchaseRequest(studentId, purchaseId);
    }

    private Purchase purchaseWithSuggestion(UUID suggestionId) {
        Purchase purchase = new Purchase();
        purchase.setSuggestionId(suggestionId);
        return purchase;
    }

    private Alumno student(int rewardPoints) {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setPuntosRecompensa(rewardPoints);
        return student;
    }
}
