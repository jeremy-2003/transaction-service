package com.bank.transactionservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@Slf4j
public class Resilience4jConfig {
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .slowCallRateThreshold(50)
                .recordExceptions(
                        IOException.class,
                        ConnectException.class,
                        WebClientResponseException.class,
                        TimeoutException.class,
                        RuntimeException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        io.github.resilience4j.circuitbreaker.CircuitBreaker accountCircuitBreaker =
                registry.circuitBreaker("accountService");
        log.info("Circuit breaker '{}' created with state: {}",
                accountCircuitBreaker.getName(), accountCircuitBreaker.getState());

        accountCircuitBreaker.getEventPublisher()
                .onStateTransition(this::logStateTransition)
                .onError(event -> log.error("Circuit breaker '{}' recorded an error: {}",
                        event.getCircuitBreakerName(), event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Circuit breaker '{}' recorded a success",
                        event.getCircuitBreakerName()));

        io.github.resilience4j.circuitbreaker.CircuitBreaker creditCircuitBreaker =
                registry.circuitBreaker("creditService");
        log.info("Circuit breaker '{}' created with state: {}",
                creditCircuitBreaker.getName(), creditCircuitBreaker.getState());

        creditCircuitBreaker.getEventPublisher()
                .onStateTransition(this::logStateTransition)
                .onError(event -> log.error("Circuit breaker '{}' recorded an error: {}",
                        event.getCircuitBreakerName(), event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Circuit breaker '{}' recorded a success",
                        event.getCircuitBreakerName()));

        io.github.resilience4j.circuitbreaker.CircuitBreaker debitCardCircuitBreaker =
                registry.circuitBreaker("debitCardService");
        log.info("Circuit breaker '{}' created with state: {}",
                debitCardCircuitBreaker.getName(), debitCardCircuitBreaker.getState());

        debitCardCircuitBreaker.getEventPublisher()
                .onStateTransition(this::logStateTransition)
                .onError(event -> log.error("Circuit breaker '{}' recorded an error: {}",
                        event.getCircuitBreakerName(), event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Circuit breaker '{}' recorded a success",
                        event.getCircuitBreakerName()));

        return registry;
    }
    private void logStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.info("CircuitBreaker '{}' transitioned from {} to {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());
    }
}
