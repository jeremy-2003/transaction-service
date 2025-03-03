package com.bank.transactionservice.client;

import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.debitcard.DebitCard;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DebitCardClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    @Autowired
    public DebitCardClientService(@Value("${account-service.base-url}") String accountServiceUrl,
                                  CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("debitCardService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    public Mono<DebitCard> getDebitCardById(String cardId) {
        return webClient.get()
                .uri("/debit-cards/{cardId}", cardId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<DebitCard>>() { })
                .flatMap(response -> {
                    if (response.getStatus() == 200 && response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.error(new RuntimeException("Debit card not found or error: "
                            + response.getMessage()));
                    }
                })
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(e -> log.error("Error retrieving debit card with ID {}: {}", cardId, e.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch debit card with ID {}. Reason: {}",
                            cardId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Debit card service is unavailable for retrieving debit card. " +
                                    "Cannot proceed with the operation."));
                });
    }
}

