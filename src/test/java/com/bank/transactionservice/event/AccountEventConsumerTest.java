package com.bank.transactionservice.event;

import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.account.AccountType;
import com.bank.transactionservice.service.TransactionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AccountEventConsumerTest {
    @Mock
    private TransactionCacheService cacheService;
    private AccountEventConsumer accountEventConsumer;
    @BeforeEach
    void setUp() {
        accountEventConsumer = new AccountEventConsumer(cacheService);
    }
    @Test
    void consumeAccountCreated_Success() {
        // Arrange
        Account account = createAccount("123");
        when(cacheService.saveAccount(account.getId(), account))
                .thenReturn(Mono.empty());
        // Act
        accountEventConsumer.consumeAccountCreated(account);
        // Assert
        verify(cacheService).saveAccount(account.getId(), account);
    }
    @Test
    void consumeAccountCreated_ErrorSavingAccount() {
        // Arrange
        Account account = createAccount("123");
        RuntimeException expectedError = new RuntimeException("Error saving account");
        when(cacheService.saveAccount(account.getId(), account))
                .thenReturn(Mono.error(expectedError));
        // Act
        accountEventConsumer.consumeAccountCreated(account);
        // Assert
        verify(cacheService).saveAccount(account.getId(), account);
    }
    @Test
    void consumeAccountCreated_NullAccount() {
        // Act
        accountEventConsumer.consumeAccountCreated(null);
        // Assert
        verify(cacheService, never()).saveAccount(any(), any());
    }
    @Test
    void consumeAccountUpdated_Success() {
        // Arrange
        Account account = createAccount("123");
        when(cacheService.saveAccount(account.getId(), account))
                .thenReturn(Mono.empty());
        // Act
        accountEventConsumer.consumeAccountUpdated(account);
        // Assert
        verify(cacheService).saveAccount(account.getId(), account);
    }
    @Test
    void consumeAccountUpdated_UnexpectedError() {
        // Arrange
        Account account = createAccount("123");
        RuntimeException unexpectedError = new RuntimeException("Unexpected error");
        when(cacheService.saveAccount(any(), any()))
                .thenThrow(unexpectedError);
        // Act
        accountEventConsumer.consumeAccountUpdated(account);
        // Assert
        verify(cacheService).saveAccount(account.getId(), account);
    }
    private Account createAccount(String id) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId("customer1");
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(1000.0);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}

