package com.bank.transactionservice.service;

import com.bank.transactionservice.model.Account;
import com.bank.transactionservice.model.AccountType;
import com.bank.transactionservice.model.Credit;
import com.bank.transactionservice.model.CreditCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccounEventConsumer {
    private final TransactionCacheService transactionCacheService;
    public AccounEventConsumer(TransactionCacheService transactionCacheService) {
        this.transactionCacheService = transactionCacheService;
    }
    @KafkaListener(topics = "account-created", groupId = "transaction-service-group")
    public void consumeAccountCreated(Account account) {
        try {
            transactionCacheService.saveAccount(account.getId(), account)
                    .doOnSuccess(unused -> log.info("Account successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving account in cache: {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing Account event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "account-updated", groupId = "transaction-service-group")
    public void consumeAccountUpdated(Account account) {
        try {
            transactionCacheService.saveAccount(account.getId(), account)
                    .doOnSuccess(unused -> log.info("Account successfully saved in cache"))
                    .doOnError(error -> log.error("Error saving account in cache: {}", error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing Account event: {}", e.getMessage());
        }
    }
}
