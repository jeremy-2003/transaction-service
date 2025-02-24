package com.bank.transactionservice.event;

import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.credit.CreditType;
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
class CreditEventConsumerTest {
    @Mock
    private TransactionCacheService cacheService;
    private CreditEventConsumer creditEventConsumer;
    @BeforeEach
    void setUp() {
        creditEventConsumer = new CreditEventConsumer(cacheService);
    }
    @Test
    void consumeCreditCreated_Success() {
        // Arrange
        Credit credit = createCredit("123");
        when(cacheService.saveCredit(credit.getId(), credit))
                .thenReturn(Mono.empty());
        // Act
        creditEventConsumer.consumeCreditCreated(credit);
        // Assert
        verify(cacheService).saveCredit(credit.getId(), credit);
    }
    @Test
    void consumeCreditCreated_ErrorSavingCredit() {
        // Arrange
        Credit credit = createCredit("123");
        RuntimeException expectedError = new RuntimeException("Error saving credit");
        when(cacheService.saveCredit(credit.getId(), credit))
                .thenReturn(Mono.error(expectedError));
        // Act
        creditEventConsumer.consumeCreditCreated(credit);
        // Assert
        verify(cacheService).saveCredit(credit.getId(), credit);
    }
    @Test
    void consumeCreditCreated_NullCredit() {
        // Act
        creditEventConsumer.consumeCreditCreated(null);
        // Assert
        verify(cacheService, never()).saveCredit(any(), any());
    }
    @Test
    void consumeCreditUpdated_Success() {
        // Arrange
        Credit credit = createCredit("123");
        when(cacheService.saveCredit(credit.getId(), credit))
                .thenReturn(Mono.empty());
        // Act
        creditEventConsumer.consumeCreditUpdated(credit);
        // Assert
        verify(cacheService).saveCredit(credit.getId(), credit);
    }
    @Test
    void consumeCreditUpdated_UnexpectedError() {
        // Arrange
        Credit credit = createCredit("123");
        RuntimeException unexpectedError = new RuntimeException("Unexpected error");
        when(cacheService.saveCredit(any(), any()))
                .thenThrow(unexpectedError);
        // Act
        creditEventConsumer.consumeCreditUpdated(credit);
        // Assert
        verify(cacheService).saveCredit(credit.getId(), credit);
    }
    private Credit createCredit(String id) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId("customer1");
        credit.setCreditType(CreditType.PERSONAL);
        credit.setAmount(new BigDecimal("5000.00"));
        credit.setRemainingBalance(new BigDecimal("5000.00"));
        credit.setCreatedAt(LocalDateTime.now());
        return credit;
    }
}
