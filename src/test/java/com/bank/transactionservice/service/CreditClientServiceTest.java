package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.credit.CreditType;
import com.bank.transactionservice.model.creditcard.CreditCard;
import com.bank.transactionservice.model.creditcard.CreditCardType;
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
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CreditClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private CreditClientService creditClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        creditClientService = new CreditClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getCreditById_Success() {
        // Arrange
        String creditId = "123";
        Credit expectedCredit = createCredit(creditId, "customer1");
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credits/{id}", creditId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<Credit> response = new BaseResponse<>();
        response.setData(expectedCredit);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditById(creditId))
                .expectNextMatches(credit ->
                        credit.getId().equals(creditId) &&
                                credit.getCustomerId().equals("customer1"))
                .verifyComplete();
    }
    @Test
    void getCreditById_NotFound() {
        // Arrange
        String creditId = "nonexistent";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credits/{id}", creditId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<Credit> response = new BaseResponse<>();
        response.setData(null);
        response.setStatus(404);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditById(creditId))
                .verifyComplete();
    }
    @Test
    void getCreditCardById_Success() {
        // Arrange
        String creditCardId = "123";
        CreditCard expectedCreditCard = createCreditCard(creditCardId, "customer1");
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credit-cards/{id}", creditCardId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<CreditCard> response = new BaseResponse<>();
        response.setData(expectedCreditCard);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardById(creditCardId))
                .expectNextMatches(creditCard ->
                        creditCard.getId().equals(creditCardId) &&
                                creditCard.getCustomerId().equals("customer1"))
                .verifyComplete();
    }
    @Test
    void updateCreditBalance_Success() {
        // Arrange
        String creditId = "123";
        BigDecimal newBalance = new BigDecimal("2000.0");
        Credit initialCredit = createCredit(creditId, "customer1");
        Credit updatedCredit = createCredit(creditId, "customer1");
        updatedCredit.setRemainingBalance(newBalance);
        // Mock for GET request
        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/credits/{id}", creditId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        BaseResponse<Credit> getResponse = new BaseResponse<>();
        getResponse.setData(initialCredit);
        getResponse.setStatus(200);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(getResponse));
        // Mock for PUT request
        WebClient.RequestBodyUriSpec putBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.put()).thenReturn(putBodyUriSpec);
        when(putBodyUriSpec.uri("/credits/{id}", creditId)).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any(Credit.class))).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.onStatus(any(), any())).thenReturn(putResponseSpec);
        BaseResponse<Credit> putResponse = new BaseResponse<>();
        putResponse.setData(updatedCredit);
        putResponse.setStatus(200);
        when(putResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(putResponse));
        // Act & Assert
        StepVerifier.create(creditClientService.updateCreditBalance(creditId, newBalance))
                .expectNextMatches(credit ->
                        credit.getId().equals(creditId) &&
                                credit.getRemainingBalance().equals(newBalance))
                .verifyComplete();
    }
    @Test
    void updateCreditCardBalance_Success() {
        // Arrange
        String creditCardId = "123";
        BigDecimal newBalance = new BigDecimal("2000.0");
        CreditCard initialCreditCard = createCreditCard(creditCardId, "customer1");
        CreditCard updatedCreditCard = createCreditCard(creditCardId, "customer1");
        updatedCreditCard.setAvailableBalance(newBalance);
        // Mock for GET request
        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/credit-cards/{id}", creditCardId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        BaseResponse<CreditCard> getResponse = new BaseResponse<>();
        getResponse.setData(initialCreditCard);
        getResponse.setStatus(200);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(getResponse));
        // Mock for PUT request
        WebClient.RequestBodyUriSpec putBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.put()).thenReturn(putBodyUriSpec);
        when(putBodyUriSpec.uri("/credit-cards/{id}", creditCardId)).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any(CreditCard.class))).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.onStatus(any(), any())).thenReturn(putResponseSpec);
        BaseResponse<CreditCard> putResponse = new BaseResponse<>();
        putResponse.setData(updatedCreditCard);
        putResponse.setStatus(200);
        when(putResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(putResponse));
        // Act & Assert
        StepVerifier.create(creditClientService.updateCreditCardBalance(creditCardId, newBalance))
                .expectNextMatches(creditCard ->
                        creditCard.getId().equals(creditCardId) &&
                                creditCard.getAvailableBalance().equals(newBalance))
                .verifyComplete();
    }
    @Test
    void updateCreditBalance_NotFound() {
        // Arrange
        String creditId = "nonexistent";
        BigDecimal newBalance = new BigDecimal("2000.0");
        // Mock for GET request
        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/credits/{id}", creditId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditClientService.updateCreditBalance(creditId, newBalance))
                .verifyComplete();
    }
    @Test
    void updateCreditCardBalance_ServerError() {
        // Arrange
        String creditCardId = "123";
        BigDecimal newBalance = new BigDecimal("2000.0");
        // Mock for GET request
        WebClient.RequestHeadersUriSpec getHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(getHeadersUriSpec);
        when(getHeadersUriSpec.uri("/credit-cards/{id}", creditCardId)).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Server error: 500")));
        // Act & Assert
        StepVerifier.create(creditClientService.updateCreditCardBalance(creditCardId, newBalance))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Server error: 500"))
                .verify();
    }
    private Credit createCredit(String id, String customerId) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId(customerId);
        credit.setCreditType(CreditType.PERSONAL);
        credit.setAmount(new BigDecimal("5000.0"));
        credit.setRemainingBalance(new BigDecimal("5000.0"));
        credit.setInterestRate(new BigDecimal("0.12"));
        credit.setCreatedAt(LocalDateTime.now());
        return credit;
    }
    private CreditCard createCreditCard(String id, String customerId) {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setCustomerId(customerId);
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        creditCard.setCreditLimit(new BigDecimal("10000.0"));
        creditCard.setAvailableBalance(new BigDecimal("10000.0"));
        creditCard.setStatus("ACTIVE");
        creditCard.setCreatedAt(LocalDateTime.now());
        return creditCard;
    }
}
