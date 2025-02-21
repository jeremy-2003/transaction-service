package com.bank.transactionservice.service;

import com.bank.transactionservice.model.account.Account;
import com.bank.transactionservice.model.credit.Credit;
import com.bank.transactionservice.model.creditcard.CreditCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
@Service
@Slf4j
public class TransactionCacheService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String ACCOUNT_KEY_PREFIX = "Account:";
    private static final String CREDIT_KEY_PREFIX = "Credit:";
    private static final String CREDIT_CARD_KEY_PREFIX = "CreditCard:";
    public TransactionCacheService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    public Mono<Void> saveAccount(String id, Account account) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Account ID cannot be null"));
        }
        String key = ACCOUNT_KEY_PREFIX + id;
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(account))
                .doOnNext(json -> log.info("Serialized Account JSON: {}", json))
                .doOnError(error -> log.error("Error serializing account: {}", error.getMessage()))
                .flatMap(accountJson -> redisTemplate.opsForValue().set(key, accountJson)
                        .doOnNext(result -> log.info("Redis SET result {}", result))
                        .doOnSuccess(result -> log.info("Successfully cached Account with ID {}", id))
                        .doOnError(error -> log.error("Error storing Account in Redis: {}", error.getMessage()))
                        .then());
    }
    public Mono<Account> getAccount(String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Account ID cannot be null"));
        }
        String key = ACCOUNT_KEY_PREFIX + id;
        log.info("Attempting to retrieve Account from Redis with key: {}", key);
        return redisTemplate.opsForValue().get(key)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> log.info("Subscribe to get Account from Redis with key: {}", key))
                .doOnNext(value -> {
                    if (value == null) {
                        log.warn("Null value retrieved from Redis for key: {}", key);
                    } else {
                        log.info("Retrieved from cache for key {}: value length={}", key, value.length());
                    }
                })
                .flatMap(accountJson -> {
                    log.info("Processing JSON for Account: {} (length: {})", key, accountJson.length());
                    try {
                        Account account = objectMapper.readValue(accountJson, Account.class);
                        log.info("Successfully deserialized Account: {}", account.getId());
                        return Mono.just(account);
                    } catch (Exception e) {
                        log.error("Error deserializing Account JSON: {}", e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(TimeoutException.class, e->
                        log.error("Redis operation timed out for key: {}", key))
                .doOnError(e->{
                    if(!(e instanceof TimeoutException)){
                        log.error("Error retrieving Account from cache: {}", e.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Final error handling for retrieving Account: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
    public Mono<Void> saveCredit(String id, Credit credit) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Credit ID cannot be null"));
        }
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(credit))
                .flatMap(creditJson -> {
                    String key = CREDIT_KEY_PREFIX + id;
                    log.info("Saving Credit to cache with key: {}", key);
                    return redisTemplate.opsForValue().set(key, creditJson);
                })
                .doOnSuccess(result -> log.info("Successfully cached Credit with ID: {}", id))
                .doOnError(error -> log.error("Error Credit: {}", error.getMessage()))
                .then();
    }
    public Mono<Credit> getCredit(String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Credit ID cannot be null"));
        }
        String key = CREDIT_KEY_PREFIX + id;
        log.info("Attempting to retrieve Credit from Redis with key: {}", key);
        return redisTemplate.opsForValue().get(key)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> log.info("Subscribe to get Credit from Redis with key: {}", key))
                .doOnNext(value -> {
                    if (value == null) {
                        log.warn("Null value retrieved from Redis for key: {}", key);
                    } else {
                        log.info("Retrieved from cache for key {}: value length={}", key, value.length());
                    }
                })
                .flatMap(creditJson -> {
                    log.info("Processing JSON for Credit: {} (length: {})", key, creditJson.length());
                    try {
                        Credit account = objectMapper.readValue(creditJson, Credit.class);
                        log.info("Successfully deserialized Credit: {}", account.getId());
                        return Mono.just(account);
                    } catch (Exception e) {
                        log.error("Error deserializing Credit JSON: {}", e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(TimeoutException.class, e->
                        log.error("Redis operation timed out for key: {}", key))
                .doOnError(e->{
                    if(!(e instanceof TimeoutException)){
                        log.error("Error retrieving Credit from cache: {}", e.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Final error handling for retrieving Credit: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
    public Mono<Void> saveCreditCard(String id, CreditCard creditCard) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("CreditCard ID cannot be null"));
        }
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(creditCard))
                .flatMap(creditCardJson -> {
                    String key = CREDIT_CARD_KEY_PREFIX + id;
                    log.info("Saving Credit to cache with key: {}", key);
                    return redisTemplate.opsForValue().set(key, creditCardJson);
                })
                .doOnSuccess(result -> log.info("Successfully cached CreditCard with ID: {}", id))
                .doOnError(error -> log.error("Error CreditCard: {}", error.getMessage()))
                .then();
    }
    public Mono<CreditCard> getCreditCard(String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("CreditCard ID cannot be null"));
        }
        String key = CREDIT_CARD_KEY_PREFIX + id;
        log.info("Attempting to retrieve CreditCard from Redis with key: {}", key);
        return redisTemplate.opsForValue().get(key)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> log.info("Subscribe to get CreditCard from Redis with key: {}", key))
                .doOnNext(value -> {
                    if (value == null) {
                        log.warn("Null value retrieved from Redis for key: {}", key);
                    } else {
                        log.info("Retrieved from cache for key {}: value length={}", key, value.length());
                    }
                })
                .flatMap(creditCardJson -> {
                    log.info("Processing JSON for CreditCard: {} (length: {})", key, creditCardJson.length());
                    try {
                        CreditCard account = objectMapper.readValue(creditCardJson, CreditCard.class);
                        log.info("Successfully deserialized CreditCard: {}", account.getId());
                        return Mono.just(account);
                    } catch (Exception e) {
                        log.error("Error deserializing CreditCard JSON: {}", e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(TimeoutException.class, e->
                        log.error("Redis operation timed out for key: {}", key))
                .doOnError(e->{
                    if(!(e instanceof TimeoutException)){
                        log.error("Error retrieving CreditCard from cache: {}", e.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Final error handling for retrieving CreditCard: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
}
