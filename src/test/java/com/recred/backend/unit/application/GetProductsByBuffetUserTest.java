package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetProductsByBuffetUserRequest;
import com.recred.backend.application.GetProductsByBuffetUserResponse;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductsByBuffetUserTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    private GetProductsByBuffetUser useCase;

    private UUID userId;
    private UUID buffetId;
    private Producto product;
    private GetProductsByBuffetUserResponse response;

    @BeforeEach
    void setUp() {
        useCase = new GetProductsByBuffetUser(productRepository, kiosqueroRepository);
    }

    @Test
    void givenBuffetUser_whenGettingProducts_thenReturnsActiveProductsWithStockFromBuffet() {
        givenProductSearchForBuffetUser();
        givenBuffetUserHasBuffet();
        givenBuffetHasActiveProductsWithStock();

        whenGettingProducts();

        thenProductIsReturned();
        thenProductsWereSearchedForUserBuffet();
    }

    @Test
    void givenNoBuffetUser_whenGettingProducts_thenReturnsAllActiveProducts() {
        givenProductSearchWithoutBuffetUser();
        givenActiveProductsExist();

        whenGettingProducts();

        thenProductIsReturned();
        thenAllActiveProductsWereSearched();
    }

    private void givenProductSearchForBuffetUser() {
        userId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        product = product();
    }

    private void givenProductSearchWithoutBuffetUser() {
        userId = null;
        product = product();
    }

    private void givenBuffetUserHasBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
    }

    private void givenBuffetHasActiveProductsWithStock() {
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
    }

    private void givenActiveProductsExist() {
        when(productRepository.findAllActive()).thenReturn(List.of(product));
    }

    private void whenGettingProducts() {
        response = useCase.execute(new GetProductsByBuffetUserRequest(userId));
    }

    private void thenProductIsReturned() {
        assertEquals(1, response.products().size());
        assertEquals(product.getId(), response.products().get(0).id());
    }

    private void thenProductsWereSearchedForUserBuffet() {
        verify(kiosqueroRepository).findBuffetByUsuarioId(userId);
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    private void thenAllActiveProductsWereSearched() {
        verifyNoInteractions(kiosqueroRepository);
        verify(productRepository).findAllActive();
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        return product;
    }
}
