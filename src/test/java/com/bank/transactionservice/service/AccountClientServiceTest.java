package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.account.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AccountClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private AccountClientService accountClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        accountClientService = new AccountClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void updateAccountBalance_Success() {
        // Arrange
        String accountId = "123";
        BigDecimal newBalance = new BigDecimal("2000.0");
        Account initialAccount = createAccount(accountId, "customer1");
        Account updatedAccount = createAccount(accountId, "customer1");
        updatedAccount.setBalance(newBalance.doubleValue());

        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/{id}", accountId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        BaseResponse<Account> getResponse = new BaseResponse<>();
        getResponse.setData(initialAccount);
        getResponse.setStatus(200);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(getResponse));

        WebClient.RequestBodyUriSpec putBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.put()).thenReturn(putBodyUriSpec);
        when(putBodyUriSpec.uri("/{id}", accountId)).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any(Account.class))).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.onStatus(any(), any())).thenReturn(putResponseSpec);
        BaseResponse<Account> putResponse = new BaseResponse<>();
        putResponse.setData(updatedAccount);
        putResponse.setStatus(200);
        when(putResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(putResponse));
        // Act & Assert
        StepVerifier.create(accountClientService.updateAccountBalance(accountId, newBalance))
                .expectNextMatches(account ->
                        account.getId().equals(accountId) &&
                                account.getBalance() == 2000.0)
                .verifyComplete();
    }
    @Test
    void updateAccountBalance_NotFound() {
        // Arrange
        String accountId = "nonexistent";
        BigDecimal newBalance = new BigDecimal("2000.0");

        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/{id}", accountId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        BaseResponse<Account> getResponse = new BaseResponse<>();
        getResponse.setData(null);
        getResponse.setStatus(404);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountClientService.updateAccountBalance(accountId, newBalance))
                .verifyComplete();
    }
    @Test
    void updateAccountBalance_ServerError() {
        // Arrange
        String accountId = "123";
        BigDecimal newBalance = new BigDecimal("2000.0");

        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/{id}", accountId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Server error: 500")));
        // Act & Assert
        StepVerifier.create(accountClientService.updateAccountBalance(accountId, newBalance))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Server error: 500"))
                .verify();
    }
    @Test
    void getAccountById_Success() {
        // Arrange
        String accountId = "123";
        Account expectedAccount = createAccount(accountId, "customer1");
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/{id}", accountId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<Account> response = new BaseResponse<>();
        response.setData(expectedAccount);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountById(accountId))
                .expectNextMatches(account ->
                        account.getId().equals(accountId) &&
                                account.getCustomerId().equals("customer1"))
                .verifyComplete();
    }
    private Account createAccount(String id, String customerId) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setBalance(1000.00);
        return account;
    }
}