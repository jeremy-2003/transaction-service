package com.bank.transactionservice.event;

import com.bank.transactionservice.model.creditcard.CreditCard;
import com.bank.transactionservice.model.creditcard.CreditCardType;
import com.bank.transactionservice.service.TransactionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CreditCardEventConsumerTest {
    @Mock
    private TransactionCacheService cacheService;
    private CreditCardEventConsumer creditCardEventConsumer;
    @BeforeEach
    void setUp() {
        creditCardEventConsumer = new CreditCardEventConsumer(cacheService);
    }
    @Test
    void consumeCreditCardCreated_Success() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        when(cacheService.saveCreditCard(creditCard.getId(), creditCard))
                .thenReturn(Mono.empty());
        // Act
        creditCardEventConsumer.consumeCreditCardCreated(creditCard);
        // Assert
        verify(cacheService).saveCreditCard(creditCard.getId(), creditCard);
    }
    @Test
    void consumeCreditCardCreated_ErrorSavingCreditCard() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        RuntimeException expectedError = new RuntimeException("Error saving credit card");
        when(cacheService.saveCreditCard(creditCard.getId(), creditCard))
                .thenReturn(Mono.error(expectedError));
        // Act
        creditCardEventConsumer.consumeCreditCardCreated(creditCard);
        // Assert
        verify(cacheService).saveCreditCard(creditCard.getId(), creditCard);
    }
    @Test
    void consumeCreditCardCreated_NullCreditCard() {
        // Act
        creditCardEventConsumer.consumeCreditCardCreated(null);
        // Assert
        verify(cacheService, never()).saveCreditCard(any(), any());
    }
    @Test
    void consumeCreditCardUpdated_Success() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        when(cacheService.saveCreditCard(creditCard.getId(), creditCard))
                .thenReturn(Mono.empty());
        // Act
        creditCardEventConsumer.consumeCreditCardUpdated(creditCard);
        // Assert
        verify(cacheService).saveCreditCard(creditCard.getId(), creditCard);
    }
    @Test
    void consumeCreditCardUpdated_UnexpectedError() {
        // Arrange
        CreditCard creditCard = createCreditCard("123");
        RuntimeException unexpectedError = new RuntimeException("Unexpected error");
        when(cacheService.saveCreditCard(any(), any()))
                .thenThrow(unexpectedError);
        // Act
        creditCardEventConsumer.consumeCreditCardUpdated(creditCard);
        // Assert
        verify(cacheService).saveCreditCard(creditCard.getId(), creditCard);
    }
    private CreditCard createCreditCard(String id) {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setCustomerId("customer1");
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        creditCard.setCreditLimit(new BigDecimal("10000.00"));
        creditCard.setAvailableBalance(new BigDecimal("10000.00"));
        creditCard.setStatus("ACTIVE");
        creditCard.setCreatedAt(LocalDateTime.now());
        return creditCard;
    }
}
