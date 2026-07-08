package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetActiveSchoolCreditRequest;
import com.recred.backend.application.GetActiveSchoolCreditResponse;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveSchoolCreditTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CREDIT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");
    private static final int INSTALLMENTS = 3;

    @Mock
    private SchoolCreditRepository repository;

    @InjectMocks
    private GetActiveSchoolCredit useCase;

    @Test
    void executeShouldReturnMappedActiveCreditWhenExists() {
        SchoolCredit credit = buildActiveCredit();
        givenActiveCreditFound(credit);

        Optional<GetActiveSchoolCreditResponse> response = whenExecute();

        thenResponseMatchesCredit(response, credit);
        thenRepositoryWasQueriedByStudentIdAndStatus();
    }

    @Test
    void executeShouldReturnEmptyWhenActiveCreditDoesNotExist() {
        givenNoActiveCreditFound();

        Optional<GetActiveSchoolCreditResponse> response = whenExecute();

        thenResponseIsEmpty(response);
        thenRepositoryWasQueriedByStudentIdAndStatus();
    }

    private SchoolCredit buildActiveCredit() {
        SchoolCredit credit = new SchoolCredit();
        credit.setId(CREDIT_ID);
        credit.setStudentId(STUDENT_ID);
        credit.setAmount(AMOUNT);
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setInstallments(INSTALLMENTS);
        return credit;
    }

    private void givenActiveCreditFound(SchoolCredit credit) {
        when(repository.findByStudentIdAndStatus(STUDENT_ID, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(credit));
    }

    private void givenNoActiveCreditFound() {
        when(repository.findByStudentIdAndStatus(STUDENT_ID, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
    }

    private Optional<GetActiveSchoolCreditResponse> whenExecute() {
        return useCase.execute(new GetActiveSchoolCreditRequest(STUDENT_ID));
    }

    private void thenResponseMatchesCredit(Optional<GetActiveSchoolCreditResponse> response, SchoolCredit credit) {
        assertTrue(response.isPresent());
        assertEquals(credit.getId(), response.get().creditId());
        assertEquals(credit.getAmount(), response.get().amount());
        assertEquals(credit.getStatus().name(), response.get().status());
        assertEquals(credit.getInstallments(), response.get().installments());
    }

    private void thenRepositoryWasQueriedByStudentIdAndStatus() {
        verify(repository).findByStudentIdAndStatus(STUDENT_ID, CreditStatus.ACTIVE);
    }

    private void thenResponseIsEmpty(Optional<GetActiveSchoolCreditResponse> response) {
        assertFalse(response.isPresent());
    }
}
