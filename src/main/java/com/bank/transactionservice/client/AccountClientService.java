package com.bank.transactionservice.client;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.account.Account;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
public class AccountClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    public AccountClientService(WebClient.Builder webClientBuilder,
                                @Value("${account-service.base-url}") String accountServiceUrl,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
        this.webClient = webClientBuilder.baseUrl(accountServiceUrl).build();
    }

    public Mono<Account> getAccountById(String accountId) {
        return webClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching account with ID {}: {}", accountId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch account with ID {}. Reason: {}",
                            accountId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Account service is unavailable for retrieving account. " +
                                    "Cannot proceed with the operation."));
                });
    }

    public Mono<Account> updateAccountBalance(String accountId, BigDecimal newBalance) {
        return getAccountById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setBalance(newBalance.doubleValue());
                    return webClient.put()
                            .uri("/accounts/{id}", accountId)
                            .bodyValue(existingAccount)
                            .retrieve()
                            .onStatus(HttpStatus::is4xxClientError, response -> {
                                log.error("Client error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                            })
                            .onStatus(HttpStatus::is5xxServerError, response -> {
                                log.error("Server error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                            })
                            .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() { })
                            .flatMap(response -> {
                                if (response.getData() != null) {
                                    return Mono.just(response.getData());
                                } else {
                                    return Mono.empty();
                                }
                            })
                            .doOnNext(result -> log.info("Account updated successfully: {}", result))
                            .doOnError(e -> log.error("Error while updating account: {}", e.getMessage()))
                            .doOnTerminate(() -> log.info("Request to update account completed"))
                            .transform(CircuitBreakerOperator.of(circuitBreaker))
                            .onErrorResume(throwable -> {
                                log.error("FALLBACK TRIGGERED: Unable to update account with ID {}. Reason: {}",
                                        accountId, throwable.getMessage());
                                log.error("Exception type: {}", throwable.getClass().getName());
                                return Mono.error(new RuntimeException(
                                        "Account service is unavailable for updating account. " +
                                                "Cannot proceed with the operation."));
                            });
                });
    }
}
