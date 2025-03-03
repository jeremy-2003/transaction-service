package com.bank.transactionservice.client;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.creditcard.CreditCard;
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

@Service
@Slf4j
public class CreditClientService {
    private final WebClient webClient;
    private final String creditServiceUrl;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    public CreditClientService(WebClient.Builder webClientBuilder,
                               @Value("${credit-service.base-url-credit}") String creditServiceUrl,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.creditServiceUrl = creditServiceUrl;
        this.webClient = webClientBuilder.baseUrl(creditServiceUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    public Mono<Credit> getCreditById(String creditId) {
        String fullUrl = creditServiceUrl + "/credits/" + creditId;
        log.info("Sending request to Credit Service API: {}", fullUrl);
        return webClient.get()
                .uri("/credits/{id}", creditId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
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
                .doOnTerminate(() -> log.info("Request to Credit API completed"))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch credit with ID {}. Reason: {}",
                            creditId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for retrieving credit. " +
                                    "Cannot proceed with the operation."));
                });
    }

    public Mono<CreditCard> getCreditCardById(String creditCardId) {
        String fullUrl = creditServiceUrl + "/credit-cards/" + creditCardId;
        log.info("Sending request to Credit Service API: {}", fullUrl);
        return webClient.get()
                .uri("/credit-cards/{id}", creditCardId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
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
                .doOnTerminate(() -> log.info("Request to CreditCard API completed"))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch credit card with ID {}. Reason: {}",
                            creditCardId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for retrieving credit card. " +
                                    "Cannot proceed with the operation."));
                });
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
                            .onStatus(HttpStatus::is4xxClientError, response -> {
                                log.error("Client error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                            })
                            .onStatus(HttpStatus::is5xxServerError, response -> {
                                log.error("Server error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                            })
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
                            .doOnTerminate(() -> log.info("Request to update Credit completed"))
                            .transform(CircuitBreakerOperator.of(circuitBreaker))
                            .onErrorResume(throwable -> {
                                log.error("FALLBACK TRIGGERED: Unable to update credit with ID {}. Reason: {}",
                                        creditId, throwable.getMessage());
                                log.error("Exception type: {}", throwable.getClass().getName());
                                return Mono.error(new RuntimeException(
                                        "Credit service is unavailable for updating credit. " +
                                                "Cannot proceed with the operation."));
                            });
                });
    }

    public Mono<CreditCard> updateCreditCardBalance(String creditCardId, BigDecimal newBalance) {
        String fullUrl = creditServiceUrl + "/credit-cards/" + creditCardId;
        log.info("Sending request to update credit card balance: {}", fullUrl);
        return getCreditCardById(creditCardId)
                .flatMap(existingCreditCard -> {
                    existingCreditCard.setAvailableBalance(newBalance);
                    return webClient.put()
                            .uri("/credit-cards/{id}", creditCardId)
                            .bodyValue(existingCreditCard)
                            .retrieve()
                            .onStatus(HttpStatus::is4xxClientError, response -> {
                                log.error("Client error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                            })
                            .onStatus(HttpStatus::is5xxServerError, response -> {
                                log.error("Server error: {}", response.statusCode());
                                return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                            })
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
                            .doOnTerminate(() -> log.info("Request to update CreditCard completed"))
                            .transform(CircuitBreakerOperator.of(circuitBreaker))
                            .onErrorResume(throwable -> {
                                log.error("FALLBACK TRIGGERED: Unable to update credit card with ID {}. Reason: {}",
                                        creditCardId, throwable.getMessage());
                                log.error("Exception type: {}", throwable.getClass().getName());
                                return Mono.error(new RuntimeException(
                                        "Credit service is unavailable for updating credit card. " +
                                                "Cannot proceed with the operation."));
                            });
                });
    }
    public Mono<Credit> updateCredit(Credit credit) {
        String fullUrl = creditServiceUrl + "/credits/" + credit.getId();
        log.info("Sending request to update credit: {}", fullUrl);
        return webClient.put()
                .uri("/credits/{id}", credit.getId())
                .bodyValue(credit)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
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
                .doOnTerminate(() -> log.info("Request to update Credit completed"))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to update credit with ID {}. Reason: {}",
                            credit.getId(), throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for updating credit. " +
                                    "Cannot proceed with the operation."));
                });
    }

}
