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
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetActiveSchoolCreditTest {

    private final SchoolCreditRepository repository = mock(SchoolCreditRepository.class);
    private final GetActiveSchoolCredit useCase = new GetActiveSchoolCredit(repository);

    @Test
    void executeShouldReturnMappedActiveCreditWhenExists() {
        UUID studentId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();
        SchoolCredit credit = new SchoolCredit();
        credit.setId(creditId);
        credit.setStudentId(studentId);
        credit.setAmount(new BigDecimal("1500.00"));
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setInstallments(3);
        GetActiveSchoolCreditRequest request = new GetActiveSchoolCreditRequest(studentId);

        when(repository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(credit));

        Optional<GetActiveSchoolCreditResponse> response = useCase.execute(request);

        assertTrue(response.isPresent());
        assertEquals(creditId, response.get().creditId());
        assertEquals(new BigDecimal("1500.00"), response.get().amount());
        assertEquals(CreditStatus.ACTIVE.name(), response.get().status());
        assertEquals(3, response.get().installments());
        verify(repository).findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE);
    }

    @Test
    void executeShouldReturnEmptyWhenActiveCreditDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        GetActiveSchoolCreditRequest request = new GetActiveSchoolCreditRequest(studentId);

        when(repository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<GetActiveSchoolCreditResponse> response = useCase.execute(request);

        assertFalse(response.isPresent());
    }

    @Test
    void executeShouldValidateRequestBeforeRepository() {
        GetActiveSchoolCreditRequest request = new GetActiveSchoolCreditRequest(null);

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("studentId cannot be null", exception.getMessage());
        verifyNoInteractions(repository);
    }
}
