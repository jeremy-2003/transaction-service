package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.creditcard.CreditCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class CreditClientService {
    private final WebClient webClient;
    private final String creditServiceUrl;
    public CreditClientService(WebClient.Builder webClientBuilder,
                                @Value("${credit-service.base-url-credit}") String creditServiceUrl) {
        this.creditServiceUrl = creditServiceUrl;
        this.webClient = webClientBuilder.baseUrl(creditServiceUrl).build();
    }
    public Mono<Credit> getCreditById(String creditId) {
        String fullUrl = creditServiceUrl + "/credits/" + creditId;
        log.info("Sending request to Credit Service API: {}", fullUrl);
        return webClient.get()
                .uri("/credits/{id}", creditId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Credit>>() { })
                .flatMap(response -> {
                    if (response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .doOnNext(result -> log.info("Credit API response: {}", result))
                .doOnError(e -> log.error("Error while fetching Credit: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to Credit API completed"));
    }
    public Mono<CreditCard> getCreditCardById(String creditCardId) {
        String fullUrl = creditServiceUrl + "/credit-cards/" + creditCardId;
        log.info("Sending request to Credit Service API: {}", fullUrl);
        return webClient.get()
                .uri("/credit-cards/{id}", creditCardId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<CreditCard>>() { })
                .flatMap(response -> {
                    if (response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .doOnNext(result -> log.info("CreditCard API response: {}", result))
                .doOnError(e -> log.error("Error while fetching CreditCard: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to CreditCard API completed"));
    }
    public Mono<Credit> updateCreditBalance(String creditId, BigDecimal newBalance) {
        String fullUrl = creditServiceUrl + "/credits/" + creditId;
        log.info("Sending request to update credit balance: {}", fullUrl);
        return getCreditById(creditId)
                .flatMap(existingCredit -> {
                    existingCredit.setRemainingBalance(newBalance);
                    return webClient.put()
                            .uri("/credits/{id}", creditId)
                            .bodyValue(existingCredit)
                            .retrieve()
                            .onStatus(HttpStatus::is4xxClientError, response ->
                                    Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                            )
                            .onStatus(HttpStatus::is5xxServerError, response ->
                                    Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                            )
                            .bodyToMono(new ParameterizedTypeReference<BaseResponse<Credit>>() { })
                            .flatMap(response -> {
                                if (response.getData() != null) {
                                    return Mono.just(response.getData());
                                } else {
                                    return Mono.empty();
                                }
                            })
                            .doOnNext(result -> log.info("Credit updated successfully: {}", result))
                            .doOnError(e -> log.error("Error while updating Credit: {}", e.getMessage()))
                            .doOnTerminate(() -> log.info("Request to update Credit completed"));
                });
    }
    public Mono<CreditCard> updateCreditCardBalance(String crediCardtId, BigDecimal newBalance) {
        String fullUrl = creditServiceUrl + "/credit-cards/" + crediCardtId;
        log.info("Sending request to update creditcard balance: {}", fullUrl);
        return getCreditCardById(crediCardtId)
                .flatMap(existingCreditCard -> {
                    existingCreditCard.setAvailableBalance(newBalance);
                    return webClient.put()
                            .uri("/credit-cards/{id}", crediCardtId)
                            .bodyValue(existingCreditCard)
                            .retrieve()
                            .onStatus(HttpStatus::is4xxClientError, response ->
                                    Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                            )
                            .onStatus(HttpStatus::is5xxServerError, response ->
                                    Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                            )
                            .bodyToMono(new ParameterizedTypeReference<BaseResponse<CreditCard>>() { })
                            .flatMap(response -> {
                                if (response.getData() != null) {
                                    return Mono.just(response.getData());
                                } else {
                                    return Mono.empty();
                                }
                            })
                            .doOnNext(result -> log.info("CreditCard updated successfully: {}", result))
                            .doOnError(e -> log.error("Error while updating CreditCard: {}", e.getMessage()))
                            .doOnTerminate(() -> log.info("Request to update CreditCard completed"));
                });
    }
}
