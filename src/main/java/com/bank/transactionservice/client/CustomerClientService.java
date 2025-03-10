package com.bank.transactionservice.client;
import com.bank.transactionservice.dto.BaseResponse;
import com.bank.transactionservice.model.customer.Customer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomerClientService {
    private final WebClient webClient;
    private final String customerServiceUrl;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    public CustomerClientService(WebClient.Builder webClientBuilder,
                                 @Value("${customer-service.base-url}") String customerServiceUrl,
                                 CircuitBreakerRegistry circuitBreakerRegistry) {
        this.customerServiceUrl = customerServiceUrl;
        this.webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("customerService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }
    public Mono<Customer> getCustomerByDocumentNumber(String documentNumber) {
        String fullUrl = customerServiceUrl + "/" + documentNumber;
        log.info("Sending request to Customer Service API: {}", fullUrl);
        return webClient.get()
                .uri("/document/{documentNumber}", documentNumber)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Customer>>() { })
                .flatMap(response -> Mono.justOrEmpty(response.getData()))
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(e -> log.error("Error while fetching customer: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to Customer API completed"))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to get customer {}. Reason: {}",
                            documentNumber, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Customer service is unavailable for retrieving customer information. " +
                                    "Cannot continue with the operation."));
                });
    }
}