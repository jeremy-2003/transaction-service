package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.account.Account;
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
    private final String accountServiceUrl;
    public AccountClientService(WebClient.Builder webClientBuilder,
                                @Value("${account-service.base-url}") String accountServiceUrl) {
        this.accountServiceUrl = accountServiceUrl;
        this.webClient = webClientBuilder.baseUrl(accountServiceUrl).build();
    }
    public Mono<Account> getAccountById(String accountId) {
        String fullUrl = accountServiceUrl + "/accounts/" + accountId;
        log.info("Sending request to Account Service API: {}", fullUrl);
        return webClient.get()
                .uri("/accounts/{id}", accountId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() { })
                .flatMap(response -> {
                    if (response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .doOnNext(result -> log.info("Account API response: {}", result))
                .doOnError(e -> log.error("Error while fetching Account: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to Account API completed"));
    }
    public Mono<Account> updateAccountBalance(String accountId, BigDecimal newBalance) {
        String fullUrl = accountServiceUrl + "/accounts/" + accountId;
        log.info("Sending request to update account balance: {}", fullUrl);
        return getAccountById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setBalance(newBalance.doubleValue());
                    return webClient.put()
                            .uri("/accounts/{id}", accountId)
                            .bodyValue(existingAccount)
                            .retrieve()
                            .onStatus(HttpStatus::is4xxClientError, response ->
                                    Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                            )
                            .onStatus(HttpStatus::is5xxServerError, response ->
                                    Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                            )
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
                            .doOnTerminate(() -> log.info("Request to update account completed"));
                });
    }
}
