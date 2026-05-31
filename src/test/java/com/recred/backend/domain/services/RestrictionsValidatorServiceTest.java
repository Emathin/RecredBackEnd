package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class RestrictionsValidatorServiceTest {

    @InjectMocks
    private RestrictionsValidatorService restrictionsValidatorService;

    @Mock
    private Student studentMock;

    @Test
    void validarRestricciones_PasaExitosamente_SiNoHayConflicto() {
        Product productSeguro = new Product();
        Set<Product> carrito = Set.of(productSeguro);

        assertDoesNotThrow(() -> restrictionsValidatorService.validatePurchase(studentMock, carrito));
    }
}